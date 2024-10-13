package it.bosler.remotealarm.data.Alarms

import it.bosler.remotealarm.R
import it.bosler.remotealarm.data.UiText

enum class Days(val value: UiText) {
    MONDAY(UiText.StringResource(R.string.monday)),
    TUESDAY(UiText.StringResource(R.string.tuesday)),
    WEDNESDAY(UiText.StringResource(R.string.wednesday)),
    THURSDAY(UiText.StringResource(R.string.thursday)),
    FRIDAY(UiText.StringResource(R.string.friday)),
    SATURDAY(UiText.StringResource(R.string.saturday)),
    SUNDAY(UiText.StringResource(R.string.sunday))
}