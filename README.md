# QMobileUI

[![Build](https://github.com/4d/android-QMobileUI/actions/workflows/build.yml/badge.svg)](https://github.com/4d/android-QMobileUI/actions/workflows/build.yml)

This android framework belong to [android SDK](https://github.com/4d/android-sdk) and it contains :
- graphical views ie. the navigation, list, details and action forms
- ...

## How it works

### Forms

List forms must inherit from [ListFormFragment](https://github.com/4d/android-QMobileUI/blob/main/qmobileui/src/main/java/com/qmobile/qmobileui/list/ListFormFragment.kt)
You will need to implement `inflateBinding()`, `initRecyclerView()`, and `initOnRefreshListener()` methods.

Example of implementations are [EntityListFragment](https://github.com/4d/android-QMobileUI/blob/main/qmobileui/src/main/java/com/qmobile/qmobileui/list/EntityListFragment.kt) and [MapsFragment](https://github.com/4d/android-QMobileUI/blob/main/qmobileui/src/main/java/com/qmobile/qmobileui/list/maps/MapsFragment.kt)

RecyclerView items are defined as [ListItemViewHolder](https://github.com/4d/android-QMobileUI/blob/main/qmobileui/src/main/java/com/qmobile/qmobileui/list/ListItemViewHolder.kt)

Detail forms inherit from [EntityDetailFragment](https://github.com/4d/android-QMobileUI/blob/main/qmobileui/src/main/java/com/qmobile/qmobileui/detail/EntityDetailFragment.kt)

### Actions

Code for action could be found in [action package](qmobileui/src/main/java/com/qmobile/qmobileui/action)

An action is described by its model [Action](qmobileui/src/main/java/com/qmobile/qmobileui/action/model/Action.kt)

An action could have some [parameters](qmobileui/src/main/java/com/qmobile/qmobileui/action/actionparameters) and
each parameters is displayed using a view that extends [BaseViewHolder](qmobileui/src/main/java/com/qmobile/qmobileui/action/actionparameters/viewholders/BaseViewHolder.kt)

#### sort

Sort action is a special action the code for sorting data could be found in this class [Sort](qmobileui/src/main/java/com/qmobile/qmobileui/action/sort/Sort.kt)

#### Offline actions

Offline action is an action that is not sent immediately (due to network problem, server not reached ...) and saved as a pending task to be sent later
Code to handle and display pending task could be found in this [pending tasks package](qmobileui/src/main/java/com/qmobile/qmobileui/action/pendingtasks)

#### Open url action

An Open url action allows user to open url in a web view inside the application (instead of external browser)
Open url action is a regular action with specified parameters, the code that open url action and web view could be found in this [webview package](qmobileui/src/main/java/com/qmobile/qmobileui/action/webview)

### Deeplink & Universal link

Deep links are a type of link that sends users directly to an app instead of a web page could be launched from and email or another app

Universal link is a variation off Deep link, but it needs some configuration on server side (see [4D-Mobile-App-Server repository](https://github.com/4d/4D-Mobile-App-Server)).

Classes that handle deep link and universal link:
- [DeepLinkUtil](qmobileui/src/main/java/com/qmobile/qmobileui/utils/DeepLinkUtil.kt)
- [DeepQueryBuilder](qmobileui/src/main/java/com/qmobile/qmobileui/utils/DeepQueryBuilder.kt)
 
## Dependencies

| Name | License | Usefulness |
|-|-|-|
| [QMobileAPI](https://github.com/4d/android-QMobileAPI) | [4D](https://github.com/4d/android-QMobileAPI/blob/master/LICENSE.md) | Network api |
| [QMobileDataStore](https://github.com/4d/android-QMobileDataStore) | [4D](https://github.com/4d/android-QMobileDataStore/blob/master/LICENSE.md) | Store data |
| [QMobileDataSync](https://github.com/4d/android-QMobileDataSync) | [4D](https://github.com/4d/android-QMobileDataSync/blob/master/LICENSE.md) | Synchronize data |
