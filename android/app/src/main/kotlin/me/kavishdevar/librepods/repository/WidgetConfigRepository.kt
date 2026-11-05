package me.kavishdevar.librepods.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.database.widget.WidgetConfigDao
import me.kavishdevar.librepods.database.widget.WidgetConfigEntity

class WidgetConfigRepository(
    private val widgetConfigDao: WidgetConfigDao
) {
    private val initialized = kotlinx.coroutines.CompletableDeferred<Unit>()
    suspend fun awaitInitialized() = initialized.await()

    private val _widgetConfigs = MutableStateFlow<List<WidgetConfigEntity>>(emptyList())
    val widgetConfigs: StateFlow<List<WidgetConfigEntity>> = _widgetConfigs.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        scope.launch(Dispatchers.IO) {
            _widgetConfigs.value = widgetConfigDao.getAll()
            initialized.complete(Unit)
        }
    }

    fun setWidgetConfig(widgetConfig: WidgetConfigEntity) {
        val existingConfig = _widgetConfigs.value.find { it.appWidgetId == widgetConfig.appWidgetId }
        if (existingConfig != null) {
            val updatedConfigs = _widgetConfigs.value.map {
                if (it.appWidgetId == widgetConfig.appWidgetId) widgetConfig else it
            }
            _widgetConfigs.value = updatedConfigs
        } else {
            _widgetConfigs.value += widgetConfig
        }

        scope.launch {
            widgetConfigDao.upsert(widgetConfig)
        }
    }
    fun delete(ids: IntArray) {
        _widgetConfigs.value = _widgetConfigs.value.filterNot { it.appWidgetId in ids }
        scope.launch { widgetConfigDao.delete(ids.toList()) }
    }

    fun restore(oldIds: IntArray, newIds: IntArray) {
        val previous = _widgetConfigs.value
        val old = oldIds.map { id -> previous.find { it.appWidgetId == id } }
        val retained = previous.filterNot { it.appWidgetId in oldIds }
        val existing = retained.map { it.appWidgetId }.toSet()
        _widgetConfigs.value = retained + newIds.zip(old).mapNotNull { (id, item) ->
            item?.takeIf { id !in existing }?.copy(appWidgetId = id)
        }
        scope.launch { widgetConfigDao.restore(oldIds.toList(), newIds.toList()) }
    }
}
