package com.readrops.db.entities.account

data class AccountConfig(
    val isFeedUrlReadOnly: Boolean, // Enable or disable feed url modification in Feed Tab
    val canCreateFolder: Boolean, // Enable or disable folder creation in Feed Tab
    val addNoFolder: Boolean, // Add a "No folder" option when modifying a feed's folder
    val useSeparateState: Boolean, // Let know if it uses ItemState table to synchronize read/star state
) {

    companion object {
        val LOCAL = AccountConfig(
            isFeedUrlReadOnly = false,
            canCreateFolder = true,
            addNoFolder = true,
            useSeparateState = false,
        )

        val NEXTCLOUD_NEWS = AccountConfig(
            isFeedUrlReadOnly = true,
            canCreateFolder = true,
            addNoFolder = true,
            useSeparateState = false,
        )

        val FRESHRSS = AccountConfig(
            isFeedUrlReadOnly = true,
            canCreateFolder = false,
            addNoFolder = false,
            useSeparateState = true,
        )

        val FEVER = AccountConfig(
            isFeedUrlReadOnly = false,
            canCreateFolder = false,
            addNoFolder = true,
            useSeparateState = true,
        )
    }
}