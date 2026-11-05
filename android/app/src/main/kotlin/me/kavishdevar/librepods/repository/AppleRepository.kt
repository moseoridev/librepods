package me.kavishdevar.librepods.repository

import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import me.kavishdevar.librepods.core.bluetooth.MacAddress
import me.kavishdevar.librepods.core.data.apple.AppleCache
import me.kavishdevar.librepods.database.apple.AppleDao
import me.kavishdevar.librepods.database.apple.AppleEntity
import me.kavishdevar.librepods.core.devices.AppleMetadata
import me.kavishdevar.librepods.core.devices.AppleSettings
import me.kavishdevar.librepods.core.devices.AppleState

//private const val TAG = "AppleRepository"
class AppleRepository(
    private val dao: AppleDao,
) {
    private val _devices = kotlinx.coroutines.flow.MutableStateFlow<Map<MacAddress, AppleEntity>>(emptyMap())
    val devices = _devices.asStateFlow()
    suspend fun initialize() { _devices.value = dao.getAll().associateBy { it.macAddress } }
    private fun cached(address: MacAddress) = _devices.value[address]
        ?: AppleEntity(address, AppleSettings(), AppleMetadata(), AppleCache())

    suspend fun load(macAddress: MacAddress): AppleEntity? {
//        Log.d(TAG, "Loading AppleEntity for $macAddress")
        val entity = dao.get(macAddress)
//        Log.i(TAG, "Loaded AppleEntity for $macAddress: $entity")
        if (entity != null) _devices.update { it + (macAddress to entity) }
        return entity
    }

    suspend fun saveSettings(macAddress: MacAddress, settings: AppleSettings) {
//        Log.d(TAG, "Saving AppleSettings for $macAddress: $settings")
        dao.updateSettings(macAddress, settings)
        _devices.update { it + (macAddress to (it[macAddress] ?: cached(macAddress)).copy(settings = settings)) }
//        Log.i(TAG, "Saved AppleSettings for $macAddress: $settings")
    }

    suspend fun saveMetadata(macAddress: MacAddress, metadata: AppleMetadata) {
//        Log.d(TAG, "Saving AppleMetadata for $macAddress: $metadata")
        dao.updateMetadata(macAddress, metadata)
        _devices.update { it + (macAddress to (it[macAddress] ?: cached(macAddress)).copy(metadata = metadata)) }
//        Log.i(TAG, "Saved AppleMetadata for $macAddress: $metadata")
    }

    suspend fun saveCache(macAddress: MacAddress, cache: AppleCache) {
//        Log.d(TAG, "Saving AppleCache for $macAddress: $cache")
        dao.saveCache(macAddress, cache)
        _devices.update { it + (macAddress to (it[macAddress] ?: cached(macAddress)).copy(cache = cache)) }
//        Log.i(TAG, "Saved AppleCache for $macAddress: $cache")
    }

    suspend fun saveCacheFromState(macAddress: MacAddress, state: AppleState) {
//        Log.d(TAG, "Saving AppleCache from AppleState for ${macAddress.toRedactedString()}")
        val cache = try {
            AppleCache(
                capabilities = state.capabilities,
                magicKeys = state.magicKeys,
                controlStates = state.controlStates,
            )
        } catch (e: Exception) {
//            Log.e(TAG, "Failed to create AppleCache from AppleState for ${macAddress.toRedactedString()}", e)
            return
        }

        saveCache(macAddress, cache)
//        Log.i(TAG, "Saved AppleCache from AppleState for ${macAddress.toRedactedString()}: $cache")
    }
}
