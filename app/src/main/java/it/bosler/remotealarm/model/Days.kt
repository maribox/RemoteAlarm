package it.bosler.remotealarm.model

import it.bosler.remotealarm.R

enum class Days(val value: UiText) {
    MONDAY(UiText.StringResource(R.string.days_monday)),
    TUESDAY(UiText.StringResource(R.string.days_tuesday)),
    WEDNESDAY(UiText.StringResource(R.string.days_wednesday)),
    THURSDAY(UiText.StringResource(R.string.days_thursday)),
    FRIDAY(UiText.StringResource(R.string.days_friday)),
    SATURDAY(UiText.StringResource(R.string.days_saturday)),
    SUNDAY(UiText.StringResource(R.string.days_sunday))
}