package com.sllauncher.pro.home

import com.sllauncher.pro.data.apps.AppKey
import com.sllauncher.pro.data.db.HomeContainer
import com.sllauncher.pro.data.db.HomeItemDao
import com.sllauncher.pro.data.db.HomeItemEntity
import com.sllauncher.pro.data.db.HomeItemType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

const val MAX_DOCK_ITEMS = 5

/** [HomeItemEntity] as a grid placement. */
val HomeItemEntity.placement: GridPlacement
    get() = object : GridPlacement {
        override val placementId = id
        override val cell = this@placement.cell
        override val spanX = this@placement.spanX
        override val spanY = this@placement.spanY
    }

/** Result of a layout change the UI may need to explain. */
enum class PlaceResult { Placed, NoSpace, AlreadyThere }

/**
 * Owns the home layout: pages, dock and folders. All writes go through one
 * mutex so two quick actions cannot place two items in the same cell.
 */
@Singleton
class HomeLayoutRepository @Inject constructor(
    private val dao: HomeItemDao,
) {
    private val mutex = Mutex()

    val items: Flow<List<HomeItemEntity>> = dao.observeAll()

    suspend fun isEmpty(): Boolean = dao.count() == 0

    suspend fun seedDock(keys: List<AppKey>) = mutex.withLock {
        if (dao.count() > 0) return@withLock
        dao.insertAll(
            keys.take(MAX_DOCK_ITEMS).mapIndexed { index, key ->
                HomeItemEntity(container = HomeContainer.DOCK, cell = index, type = HomeItemType.APP, appKey = key.flatten())
            },
        )
    }

    /** Adds an app to the first free cell, starting at [preferredPage]. Adds a page when all are full. */
    suspend fun addApp(key: AppKey, grid: GridSpec, pageCount: Int, preferredPage: Int = 0): PlaceResult = mutex.withLock {
        val all = dao.getAll()
        if (all.any { it.container == HomeContainer.PAGE && it.appKey == key.flatten() }) return@withLock PlaceResult.AlreadyThere
        val (page, cell) = freeSpot(all, grid, pageCount, preferredPage, 1, 1)
        dao.insert(HomeItemEntity(container = HomeContainer.PAGE, page = page, cell = cell, type = HomeItemType.APP, appKey = key.flatten()))
        PlaceResult.Placed
    }

    suspend fun addToDock(key: AppKey): PlaceResult = mutex.withLock {
        val dock = dao.getAll().filter { it.container == HomeContainer.DOCK }
        if (dock.any { it.appKey == key.flatten() }) return@withLock PlaceResult.AlreadyThere
        if (dock.size >= MAX_DOCK_ITEMS) return@withLock PlaceResult.NoSpace
        dao.insert(HomeItemEntity(container = HomeContainer.DOCK, cell = (dock.maxOfOrNull { it.cell } ?: -1) + 1, type = HomeItemType.APP, appKey = key.flatten()))
        PlaceResult.Placed
    }

    suspend fun addShortcut(packageName: String, id: String, userSerial: Long, label: String, grid: GridSpec, pageCount: Int): PlaceResult = mutex.withLock {
        val all = dao.getAll()
        val (page, cell) = freeSpot(all, grid, pageCount, 0, 1, 1)
        dao.insert(
            HomeItemEntity(
                container = HomeContainer.PAGE, page = page, cell = cell, type = HomeItemType.SHORTCUT,
                shortcutPackage = packageName, shortcutId = id, userSerial = userSerial, label = label,
            ),
        )
        PlaceResult.Placed
    }

    /** Places a bound widget. Returns the page it landed on. */
    suspend fun addWidget(widgetId: Int, spanX: Int, spanY: Int, grid: GridSpec, pageCount: Int, preferredPage: Int): Int = mutex.withLock {
        val all = dao.getAll()
        val (page, cell) = freeSpot(all, grid, pageCount, preferredPage, spanX, spanY)
        dao.insert(HomeItemEntity(container = HomeContainer.PAGE, page = page, cell = cell, spanX = spanX, spanY = spanY, type = HomeItemType.WIDGET, widgetId = widgetId))
        page
    }

    /**
     * Moves an item to [page]/[cell]. If a single-cell item is there and the
     * moving item is also single-cell, the two swap places.
     */
    suspend fun move(itemId: Long, page: Int, cell: Int, grid: GridSpec): Boolean = mutex.withLock {
        val all = dao.getAll()
        val item = all.firstOrNull { it.id == itemId } ?: return@withLock false
        val pageItems = all.filter { it.container == HomeContainer.PAGE && it.page == page }
        val moved = item.copy(container = HomeContainer.PAGE, page = page, cell = cell, folderId = null)
        if (canPlace(pageItems.map { it.placement }, grid, cell, item.spanX, item.spanY, excludeId = item.id)) {
            dao.update(moved)
            return@withLock true
        }
        val occupant = pageItems.firstOrNull { it.id != item.id && cell in coveredCells(it.cell, it.spanX, it.spanY, grid.columns) }
        if (occupant != null && occupant.spanX == 1 && occupant.spanY == 1 && item.spanX == 1 && item.spanY == 1 && item.container == HomeContainer.PAGE) {
            dao.updateAll(listOf(moved, occupant.copy(page = item.page, cell = item.cell)))
            return@withLock true
        }
        false
    }

    suspend fun resizeWidget(itemId: Long, spanX: Int, spanY: Int, grid: GridSpec): Boolean = mutex.withLock {
        val all = dao.getAll()
        val item = all.firstOrNull { it.id == itemId } ?: return@withLock false
        val pageItems = all.filter { it.container == HomeContainer.PAGE && it.page == item.page }
        if (!canPlace(pageItems.map { it.placement }, grid, item.cell, spanX, spanY, excludeId = item.id)) return@withLock false
        dao.update(item.copy(spanX = spanX, spanY = spanY))
        true
    }

    suspend fun remove(itemId: Long) = mutex.withLock { dao.deleteWithChildren(itemId) }

    /** Drops every shortcut to an app that was uninstalled. */
    suspend fun removeApp(key: String) = mutex.withLock { dao.deleteAppEverywhere(key) }

    /** Makes a new folder from [first] and [second] at [first]'s position. */
    suspend fun createFolder(firstId: Long, secondId: Long, name: String): Long? = mutex.withLock {
        val all = dao.getAll()
        val first = all.firstOrNull { it.id == firstId } ?: return@withLock null
        val second = all.firstOrNull { it.id == secondId } ?: return@withLock null
        if (first.type != HomeItemType.APP || second.type != HomeItemType.APP) return@withLock null
        val folderId = dao.insert(
            HomeItemEntity(container = first.container, page = first.page, cell = first.cell, type = HomeItemType.FOLDER, label = name),
        )
        dao.updateAll(
            listOf(
                first.copy(container = HomeContainer.FOLDER, folderId = folderId, cell = 0, page = 0),
                second.copy(container = HomeContainer.FOLDER, folderId = folderId, cell = 1, page = 0),
            ),
        )
        folderId
    }

    suspend fun moveIntoFolder(itemId: Long, folderId: Long): Boolean = mutex.withLock {
        val all = dao.getAll()
        val item = all.firstOrNull { it.id == itemId && it.type == HomeItemType.APP } ?: return@withLock false
        val nextCell = (all.filter { it.folderId == folderId }.maxOfOrNull { it.cell } ?: -1) + 1
        dao.update(item.copy(container = HomeContainer.FOLDER, folderId = folderId, cell = nextCell, page = 0))
        true
    }

    /** Adds an app from the drawer straight into a folder. */
    suspend fun addAppToFolder(key: AppKey, folderId: Long) = mutex.withLock {
        val all = dao.getAll()
        if (all.any { it.folderId == folderId && it.appKey == key.flatten() }) return@withLock
        val nextCell = (all.filter { it.folderId == folderId }.maxOfOrNull { it.cell } ?: -1) + 1
        dao.insert(HomeItemEntity(container = HomeContainer.FOLDER, folderId = folderId, cell = nextCell, type = HomeItemType.APP, appKey = key.flatten()))
    }

    /** Takes an app out of its folder and back onto a page. Empty folders are deleted. */
    suspend fun removeFromFolder(itemId: Long, grid: GridSpec, pageCount: Int) = mutex.withLock {
        val all = dao.getAll()
        val item = all.firstOrNull { it.id == itemId } ?: return@withLock
        val folder = all.firstOrNull { it.id == item.folderId } ?: return@withLock
        val (page, cell) = freeSpot(all.filter { it.id != item.id }, grid, pageCount, folder.page, 1, 1)
        dao.update(item.copy(container = HomeContainer.PAGE, folderId = null, page = page, cell = cell))
        if (all.count { it.folderId == folder.id } <= 1) dao.deleteWithChildren(folder.id)
    }

    suspend fun renameFolder(folderId: Long, name: String) = mutex.withLock {
        dao.getAll().firstOrNull { it.id == folderId }?.let { dao.update(it.copy(label = name)) }
    }

    /**
     * After the user shrinks the grid, items that no longer fit are moved to
     * the next free spot (on a new page if needed). Nothing is deleted.
     */
    suspend fun reflow(grid: GridSpec, pageCount: Int): Int = mutex.withLock {
        val all = dao.getAll().toMutableList()
        var pages = pageCount
        val misplaced = all.filter { it.container == HomeContainer.PAGE && !fitsInGrid(it.cell, it.spanX, it.spanY, grid) }
        for (item in misplaced) {
            all.remove(item)
            val spanX = item.spanX.coerceAtMost(grid.columns)
            val spanY = item.spanY.coerceAtMost(grid.rows)
            val (page, cell) = freeSpot(all, grid, pages, item.page, spanX, spanY)
            pages = maxOf(pages, page + 1)
            val fixed = item.copy(page = page, cell = cell, spanX = spanX, spanY = spanY)
            dao.update(fixed)
            all.add(fixed)
        }
        pages
    }

    suspend fun snapshot(): List<HomeItemEntity> = dao.getAll()

    suspend fun restore(items: List<HomeItemEntity>) = mutex.withLock { dao.replaceAll(items) }

    private fun freeSpot(all: List<HomeItemEntity>, grid: GridSpec, pageCount: Int, preferredPage: Int, spanX: Int, spanY: Int): Pair<Int, Int> {
        val lastPage = maxOf(pageCount - 1, all.filter { it.container == HomeContainer.PAGE }.maxOfOrNull { it.page } ?: 0)
        val order = listOf(preferredPage) + (0..lastPage).filter { it != preferredPage }
        for (page in order) {
            val pageItems = all.filter { it.container == HomeContainer.PAGE && it.page == page }.map { it.placement }
            findFreeCell(pageItems, grid, spanX, spanY)?.let { return page to it }
        }
        return (lastPage + 1) to 0
    }
}
