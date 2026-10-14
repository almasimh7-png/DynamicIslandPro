package com.ziba.dynamicislandpro.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.ziba.dynamicislandpro.core.AppBus

/**
 * Accessibility-service SKELETON for detecting call UI events.
 *
 * Documented limitations:
 *  - Many dialers (Google, Samsung) run in a separate user profile and their
 *    window-change events are NOT delivered to third-party accessibility services.
 *  - Cannot read/answer calls; can only infer INCOMING/ACTIVE/ENDED from package
 *    window transitions of well-known dialer packages.
 *  - Reliable call state should come from CallStyle notifications via
 *    IslandNotificationListener, or from an InCallService (requires default-dialer role).
 */
class CallAccessibilityService : AccessibilityService() {

    private val dialerPackages = setOf(
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.android.dialer",
        "com.android.incallui"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in dialerPackages) return
        // Heuristic: window appears -> call UI active; disappears -> handled by
        // notification removal in IslandNotificationListener (ENDED).
        AppBus.pushCallState(AppBus.CallState.ACTIVE)
    }

    override fun onInterrupt() {}
}
