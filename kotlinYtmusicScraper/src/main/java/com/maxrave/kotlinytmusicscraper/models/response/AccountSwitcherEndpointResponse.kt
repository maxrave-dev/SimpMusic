package com.maxrave.kotlinytmusicscraper.models.response

import com.maxrave.kotlinytmusicscraper.models.AccountInfo
import com.maxrave.kotlinytmusicscraper.models.Run
import com.maxrave.kotlinytmusicscraper.models.Thumbnail
import kotlinx.serialization.Serializable

@Serializable
data class AccountSwitcherEndpointResponse(
    val code: String?,
    val data: Data?,
) {
    @Serializable
    data class Data(
        val actions: List<Action?>?,
        val contents: List<Action.GetMultiPageMenuAction.Menu.MultiPageMenuRenderer.Section>?,
        val responseContext: ResponseContext?,
        val selectText: SelectText?,
    ) {
        @Serializable
        data class Action(
            val getMultiPageMenuAction: GetMultiPageMenuAction?,
        ) {
            @Serializable
            data class GetMultiPageMenuAction(
                val menu: Menu?,
            ) {
                @Serializable
                data class Menu(
                    val multiPageMenuRenderer: MultiPageMenuRenderer?,
                ) {
                    @Serializable
                    data class MultiPageMenuRenderer(
                        val footer: Footer?,
                        val header: Header?,
                        val sections: List<Section?>?,
                        val style: String?,
                    ) {
                        @Serializable
                        data class Footer(
                            val multiPageMenuSectionRenderer: MultiPageMenuSectionRenderer?,
                        ) {
                            @Serializable
                            data class MultiPageMenuSectionRenderer(
                                val items: List<Item?>?,
                            ) {
                                @Serializable
                                data class Item(
                                    val compactLinkRenderer: CompactLinkRenderer?,
                                ) {
                                    @Serializable
                                    data class CompactLinkRenderer(
                                        val icon: Icon?,
                                        val navigationEndpoint: NavigationEndpoint?,
                                        val style: String?,
                                        val title: Title?,
                                    ) {
                                        @Serializable
                                        data class Icon(
                                            val iconType: String?,
                                        )

                                        @Serializable
                                        data class NavigationEndpoint(
                                            val signOutEndpoint: SignOutEndpoint?,
                                            val urlEndpoint: UrlEndpoint?,
                                        ) {
                                            @Serializable
                                            data class SignOutEndpoint(
                                                val hack: Boolean?,
                                            )

                                            @Serializable
                                            data class UrlEndpoint(
                                                val url: String?,
                                            )
                                        }

                                        @Serializable
                                        data class Title(
                                            val runs: List<Run?>?,
                                        )
                                    }
                                }
                            }
                        }

                        @Serializable
                        data class Header(
                            val simpleMenuHeaderRenderer: SimpleMenuHeaderRenderer?,
                        ) {
                            @Serializable
                            data class SimpleMenuHeaderRenderer(
                                val backButton: BackButton?,
                                val title: Title?,
                            ) {
                                @Serializable
                                data class BackButton(
                                    val buttonRenderer: ButtonRenderer?,
                                ) {
                                    @Serializable
                                    data class ButtonRenderer(
                                        val accessibility: Accessibility?,
                                        val accessibilityData: AccessibilityData?,
                                        val icon: Icon?,
                                        val isDisabled: Boolean?,
                                        val size: String?,
                                        val style: String?,
                                    ) {
                                        @Serializable
                                        data class Accessibility(
                                            val label: String?,
                                        )

                                        @Serializable
                                        data class AccessibilityData(
                                            val accessibilityData: AccessibilityData?,
                                        ) {
                                            @Serializable
                                            data class AccessibilityData(
                                                val label: String?,
                                            )
                                        }

                                        @Serializable
                                        data class Icon(
                                            val iconType: String?,
                                        )
                                    }
                                }

                                @Serializable
                                data class Title(
                                    val runs: List<Run?>?,
                                )
                            }
                        }

                        @Serializable
                        data class Section(
                            val accountSectionListRenderer: AccountSectionListRenderer?,
                        ) {
                            @Serializable
                            data class AccountSectionListRenderer(
                                val contents: List<Content?>?,
                                val header: Header?,
                            ) {
                                @Serializable
                                data class Content(
                                    val accountItemSectionRenderer: AccountItemSectionRenderer?,
                                ) {
                                    @Serializable
                                    data class AccountItemSectionRenderer(
                                        val contents: List<Content?>?,
                                        val header: Header?,
                                    ) {
                                        @Serializable
                                        data class Content(
                                            val accountItem: AccountItem?,
                                        ) {
                                            @Serializable
                                            data class AccountItem(
                                                val onBehalfOfParameter: String?,
                                                val accountByline: AccountByline?,
                                                val accountLogDirectiveInts: List<Int?>?,
                                                val accountName: AccountName?,
                                                val accountPhoto: AccountPhoto?,
                                                val channelHandle: ChannelHandle?,
                                                val hasChannel: Boolean?,
                                                val isDisabled: Boolean?,
                                                val isSelected: Boolean?,
                                                val mobileBanner: MobileBanner?,
                                                val serviceEndpoint: ServiceEndpoint?,
                                                val unlimitedStatus: List<UnlimitedStatus?>?,
                                            ) {
                                                @Serializable
                                                data class AccountByline(
                                                    val simpleText: String,
                                                )

                                                @Serializable
                                                data class AccountName(
                                                    val simpleText: String,
                                                )

                                                @Serializable
                                                data class AccountPhoto(
                                                    val thumbnails: List<Thumbnail?>?,
                                                )

                                                @Serializable
                                                data class ChannelHandle(
                                                    val simpleText: String,
                                                )

                                                @Serializable
                                                data class MobileBanner(
                                                    val thumbnails: List<Thumbnail?>?,
                                                )

                                                @Serializable
                                                data class ServiceEndpoint(
                                                    val selectActiveIdentityEndpoint: SelectActiveIdentityEndpoint?,
                                                )

                                                @Serializable
                                                data class UnlimitedStatus(
                                                    val runs: List<Run?>?,
                                                )

                                                fun toAccountInfo(email: String): AccountInfo? {
                                                    return AccountInfo(
                                                        name = accountName?.simpleText ?: return null,
                                                        email = email,
                                                        pageId =
                                                            onBehalfOfParameter
                                                                ?: serviceEndpoint
                                                                    ?.selectActiveIdentityEndpoint
                                                                    ?.supportedTokens
                                                                    ?.firstOrNull { it?.pageIdToken != null }
                                                                    ?.pageIdToken
                                                                    ?.pageId,
                                                        thumbnails = accountPhoto?.thumbnails?.filterNotNull() ?: emptyList(),
                                                    )
                                                }
                                            }
                                        }

                                        @Serializable
                                        data class Header(
                                            val accountItemSectionHeaderRenderer: AccountItemSectionHeaderRenderer?,
                                        ) {
                                            @Serializable
                                            data class AccountItemSectionHeaderRenderer(
                                                val title: Title?,
                                            ) {
                                                @Serializable
                                                data class Title(
                                                    val runs: List<Run?>?,
                                                )
                                            }
                                        }
                                    }
                                }

                                @Serializable
                                data class Header(
                                    val accountsDialogHeaderRenderer: AccountsDialogHeaderRenderer?,
                                    val googleAccountHeaderRenderer: GoogleAccountHeaderRenderer?,
                                ) {
                                    @Serializable
                                    data class AccountsDialogHeaderRenderer(
                                        val text: Text?,
                                    ) {
                                        @Serializable
                                        data class Text(
                                            val runs: List<Run?>?,
                                        )
                                    }

                                    @Serializable
                                    data class GoogleAccountHeaderRenderer(
                                        val email: Email?,
                                        val name: Name?,
                                    ) {
                                        @Serializable
                                        data class Email(
                                            val runs: List<Run?>?,
                                        )

                                        @Serializable
                                        data class Name(
                                            val runs: List<Run?>?,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        @Serializable
        data class ResponseContext(
            val serviceTrackingParams: List<ServiceTrackingParam?>?,
        ) {
            @Serializable
            data class ServiceTrackingParam(
                val params: List<Param?>?,
                val service: String?,
            ) {
                @Serializable
                data class Param(
                    val key: String?,
                    val value: String?,
                )
            }
        }

        @Serializable
        data class SelectText(
            val runs: List<Run?>?,
        )
    }
}

@Serializable
data class SelectActiveIdentityEndpoint(
    val supportedTokens: List<SupportedToken?>?,
) {
    @Serializable
    data class SupportedToken(
        val accountSigninToken: AccountSigninToken?,
        val accountStateToken: AccountStateToken?,
        val datasyncIdToken: DatasyncIdToken?,
        val offlineCacheKeyToken: OfflineCacheKeyToken?,
        val pageIdToken: PageIdToken?,
    ) {
        @Serializable
        data class AccountSigninToken(
            val signinUrl: String?,
        )

        @Serializable
        data class AccountStateToken(
            val hasChannel: Boolean?,
            val isMerged: Boolean?,
            val obfuscatedGaiaId: String?,
        )

        @Serializable
        data class DatasyncIdToken(
            val datasyncIdToken: String?,
        )

        @Serializable
        data class OfflineCacheKeyToken(
            val clientCacheKey: String?,
        )

        @Serializable
        data class PageIdToken(
            val pageId: String?,
        )
    }
}

fun AccountSwitcherEndpointResponse.toListAccountInfo(): List<AccountInfo> {
    if (this.code == "SUCCESS" && this.data != null) {
        val list = mutableListOf<AccountInfo>()
        this.data.contents
            ?.firstOrNull()
            ?.accountSectionListRenderer
            ?.contents
            ?.firstOrNull()
            ?.accountItemSectionRenderer
            ?.contents
            ?.forEach { content ->
                content?.accountItem?.let { accountItem ->
                    accountItem
                        .toAccountInfo(
                            email =
                                accountItem.channelHandle
                                    ?.simpleText ?: "",
                        )?.let {
                            list.add(it)
                        }
                }
            }
        return list
    } else {
        return emptyList()
    }
}