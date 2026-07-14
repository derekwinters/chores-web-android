package com.derekwinters.chores.data.local

/**
 * In-memory [ConnectionStatusStore] + [PostedNotificationsStore] test double (issue #43),
 * mirroring production's single-class-implements-both shape ([SharedPrefsConnectionStatusStore]).
 */
class FakeConnectionStatusStore(
    initialPostedIds: Set<Int> = emptySet()
) : ConnectionStatusStore, PostedNotificationsStore {

    var lastContactMillis: Long? = null
        private set
    var recordContactCallCount: Int = 0
        private set

    private val postedIds: MutableSet<Int> = initialPostedIds.toMutableSet()

    override fun recordSuccessfulContact(epochMillis: Long) {
        recordContactCallCount++
        lastContactMillis = epochMillis
    }

    override fun lastSuccessfulContact(): Long? = lastContactMillis

    override fun isPosted(notificationId: Int): Boolean = postedIds.contains(notificationId)

    override fun markPosted(notificationId: Int) {
        postedIds.add(notificationId)
    }

    fun postedIds(): Set<Int> = postedIds.toSet()
}
