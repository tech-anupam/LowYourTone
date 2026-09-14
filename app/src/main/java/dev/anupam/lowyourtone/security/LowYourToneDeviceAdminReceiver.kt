package dev.anupam.lowyourtone.security

import android.app.admin.DeviceAdminReceiver

/**
 * Android requires this registered receiver and an explicit user approval before
 * an app may lock the device with DevicePolicyManager.lockNow().
 */
class LowYourToneDeviceAdminReceiver : DeviceAdminReceiver()
