# QMobileUI

[![Build](https://github.com/4d/android-QMobileUI/actions/workflows/build.yml/badge.svg)](https://github.com/4d/android-QMobileUI/actions/workflows/build.yml)

This android framework belong to [android SDK](https://github.com/4d/android-sdk) and it contains :
- graphical views ie. the navigation, list, details and action forms
- ...

## How it workds

### Forms

List forms must inherit from [ListFormFragment](https://github.com/4d/android-QMobileUI/blob/main/qmobileui/src/main/java/com/qmobile/qmobileui/list/ListFormFragment.kt)
You will need to implement `inflateBinding()`, `initRecyclerView()`, and `initOnRefreshListener()` methods.

Example of implementations are [EntityListFragment](https://github.com/4d/android-QMobileUI/blob/main/qmobileui/src/main/java/com/qmobile/qmobileui/list/EntityListFragment.kt) and [MapsFragment](https://github.com/4d/android-QMobileUI/blob/main/qmobileui/src/main/java/com/qmobile/qmobileui/list/maps/MapsFragment.kt)

RecyclerView items are defined as [ListItemViewHolder](https://github.com/4d/android-QMobileUI/blob/main/qmobileui/src/main/java/com/qmobile/qmobileui/list/ListItemViewHolder.kt)

Detail forms inherit from [EntityDetailFragment](https://github.com/4d/android-QMobileUI/blob/main/qmobileui/src/main/java/com/qmobile/qmobileui/detail/EntityDetailFragment.kt)



### Actions

🚧

### 🚧



## Dependencies

| Name | License | Usefulness |
|-|-|-|
| [QMobileAPI](https://github.com/4d/android-QMobileAPI) | [4D](https://github.com/4d/android-QMobileAPI/blob/master/LICENSE.md) | Network api |
| [QMobileDataStore](https://github.com/4d/android-QMobileDataStore) | [4D](https://github.com/4d/android-QMobileDataStore/blob/master/LICENSE.md) | Store data |
| [QMobileDataSync](https://github.com/4d/android-QMobileDataSync) | [4D](https://github.com/4d/android-QMobileDataSync/blob/master/LICENSE.md) | Synchronize data |
