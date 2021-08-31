// @license http://www.gnu.org/licenses/agpl-3.0.html AGPL-3.0

/*! SavaPage jQuery Mobile Admin POS Page | (c) 2020 Datraverse B.V. | GNU
 * Affero General Public License */

/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */

/*
 * NOTE: the *! comment blocks are part of the compressed version.
 */

/*jslint browser: true*/
/*global $, jQuery, alert*/

/*
 * SavaPage jQuery Mobile Admin Pages.
 */
(function($, window, document, JSON, _ns) {
    "use strict";

    // =========================================================================
    /**
     * Constructor
     */
    _ns.PagePointOfSale = function(_i18n, _view, _model, _api, isMain) {
        var _page = new _ns.Page(_i18n, _view, "#page-point-of-sale",
            (isMain ? "PagePointOfSaleMain" : "PagePointOfSalePage")),
            _self = _ns.derive(_page),
            _quickUserSelected,
            _quickUserSearch = new _ns.QuickObjectSearch(_view, _api),
            _onQuickSearchUserBefore = function() {
                $(".sp-pos-user-selected").hide();
            },
            _onQuickSearchUserItemDisplay = function(item) {
                return item.text + " &bull; " + (item.email || "&nbsp;");
            },
            _onSelectUser = function(quickUserSelected) {

                var attr = "data-savapage",
                    sel = $("#sp-pos-userid");

                _quickUserSelected = quickUserSelected;

                sel.attr(attr, quickUserSelected.key);
                sel.val(quickUserSelected.text);

                $("#sp-pos-user-balance").text(quickUserSelected.balance);
                $("#sp-pos-receipt-as-email-label").html(quickUserSelected.email || "&nbsp;");
                $(".sp-pos-user-selected").show();

                $("#sp-pos-amount-main").focus();
            },
            _onClearUser = function() {
                $.noop();
            },
            _clearSales = function() {
                $("#sp-pos-sales-amount-cents").val("00");
                $("#sp-pos-sales-comment").val("");
                _view.asyncFocus($("#sp-pos-sales-amount-main").val(""));
            },
            _clear = function() {
                $("#sp-pos-userid").val("").focus();
                $("#sp-pos-amount-main").val("");
                $("#sp-pos-comment").val("");
                $("#sp-pos-amount-cents").val("00");
                $(".sp-pos-user-selected").hide();
            },
            _onSales = function(userKey, userId) {
                var res = _api.call({
                    request: "pos-sales",
                    dto: JSON.stringify({
                        userKey: userKey,
                        accountContext: $('#sp-pos-sales-account').val(),
                        userId: userId,
                        amountMain: $("#sp-pos-sales-amount-main").val(),
                        amountCents: $("#sp-pos-sales-amount-cents").val(),
                        comment: $("#sp-pos-sales-comment").val(),
                        invoiceDelivery: undefined
                    })
                });

                _view.showApiMsg(res);

                if (res.result.code === '0') {

                    if (_api.call({
                        request: "user-notify-account-change",
                        dto: JSON.stringify({
                            key: userKey
                        })
                    }).result.code !== '0') {
                        _view.showApiMsg(res);
                    }
                    _clearSales();
                }
            },
            _onDeposit = function() {
                var sel = $('#sp-pos-payment-type')
                    // PosDepositDto
                    ,
                    res = _api.call({
                        request: "pos-deposit",
                        dto: JSON.stringify({
                            userId: _quickUserSelected.text,
                            amountMain: $("#sp-pos-amount-main").val(),
                            amountCents: $("#sp-pos-amount-cents").val(),
                            comment: $("#sp-pos-comment").val(),
                            paymentType: (sel ? sel.val() : undefined),
                            receiptDelivery: _view.getRadioValue('sp-pos-receipt-delivery'),
                            userEmail: _quickUserSelected.email
                        })
                    });

                _view.showApiMsg(res);

                if (res.result.code === '0') {

                    if (_api.call({
                        request: "user-notify-account-change",
                        dto: JSON.stringify({
                            key: _quickUserSelected.key
                        })
                    }).result.code !== '0') {
                        _view.showApiMsg(res);
                    }

                    _clear();
                }
            },
            // Get Date as yyyymmdd. Usage: _getQuickDate(new Date())
            _getQuickDate = function(date) {
                var yyyy = date.getFullYear().toString(),
                    mm = (date.getMonth() + 1).toString(),
                    // getMonth() is zero-based
                    dd = date.getDate().toString();
                // padding
                return yyyy + (mm[1] ? mm : "0" + mm[0]) + (dd[1] ? dd : "0" + dd[0]);
            },
            _onQuickPurchaseSearch = function(target, filter) {
                /* QuickSearchFilterDto */
                var res,
                    btnCls = "ui-btn ui-btn-inline ui-btn-icon-left ui-mini",
                    html = "";

                if (filter && filter.length > 0) {
                    res = _api.call({
                        request: "pos-deposit-quick-search",
                        dto: JSON.stringify({
                            filter: filter,
                            maxResults: 20
                        })
                    });
                    if (res.result.code === '0') {

                        $.each(res.dto.items, function(key, item) {

                            // item = QuickSearchPosPurchaseItemDto

                            html += "<li>";
                            html += "<h3 class=\"sp-txt-wrap\">" + item.userId + "</h3>";
                            html += "<div class=\"sp-txt-wrap\">" + item.totalCost;
                            if (item.comment) {
                                html += " &bull; " + item.comment;
                            }
                            html += "</div>";

                            // Download + mail buttons
                            html += "<div>";
                            html += "<a tabindex=\"0\" href=\"#\" data-savapage=\"" + item.key + "\" class=\"sp-download-receipt ui-icon-arrow-d " + btnCls + "\">PDF</a>";
                            if (item.userEmail) {
                                html += "<a tabindex=\"0\" href=\"#\" data-savapage=\"" + item.key + "\" class=\"sp-download-mail ui-icon-mail " + btnCls + "\">" + item.userEmail + "</a>";
                            }
                            html += "</div>";

                            /*
                             * IMPORTANT: The filter MUST be part of the item
                             * text. If filter is NOT part of the item, the item
                             * is hidden by JQM, because of <input
                             * data-type="search"
                             */
                            html += "<span style=\"font-size:0px;\">" + filter + "</span>";
                            html += "<p class=\"ui-li-aside\">" + item.dateTime + "</p>";

                            html += "</li>";
                        });
                    } else {
                        _view.showApiMsg(res);
                    }
                }
                target.html(html).filterable("refresh");
            };

        /**
         *
         */
        $(_self.id()).on('pagecreate', function(event) {

            var filterableDateTime = $("#sp-pos-quickdate-filter");

            $(this).on('click', '#sp-pos-button-deposit', null, function() {
                _onDeposit();
            });

            $(this).on('click', '#sp-pos-button-clear', null, function() {
                _clear();
            }).on('click', '#sp-pos-sales-button-clear', null, function() {
                _clearSales();
            }).on('click', "#sp-pos-tab-sales-button", null, function() {
                _clearSales();
            });

            $(this).on('click', "#sp-pos-tab-deposit-button", null, function() {
                _view.asyncFocus($("#sp-pos-userid").val(""));
            });

            $(this).on('click', "#sp-pos-tab-receipts-button", null, function() {
                var value = _getQuickDate(new Date());
                $("#sp-pos-quickdate").val(value).focus();
                _onQuickPurchaseSearch(filterableDateTime, value);
            });

            // Receipts tab
            filterableDateTime.on("filterablebeforefilter", function(e, data) {
                _onQuickPurchaseSearch($(this), data.input.get(0).value);
            });

            $(this).on('click', ".sp-download-receipt", null, function() {
                _api.download("pos-receipt-download", null, $(this).attr('data-savapage'));
                return false;
            });

            $(this).on('click', ".sp-download-mail", null, function() {
                var res = _api.call({
                    request: "pos-receipt-sendmail",
                    // PrimaryKeyDto
                    dto: JSON.stringify({
                        key: $(this).attr('data-savapage')
                    })
                });
                _view.showApiMsg(res);
                return false;
            });

            $(this).on('click', ".sp-pos-button-back", null, function() {
                if (_self.onBack) {
                    return _self.onBack();
                }
                return true;
            });

            _quickUserSearch.onCreate($(this), 'sp-pos-userid-filter', 'user-quick-search', null, _onQuickSearchUserItemDisplay, _onSelectUser, _onClearUser, _onQuickSearchUserBefore);

            _ns.KeyboardLogger.setCallback($('#sp-pos-sales-user-card-local-group'), _model.cardLocalMaxMsecs,
                //
                function() {// focusIn
                    $('#sp-pos-sales-user-card-local-focusin').show();
                    $('#sp-pos-sales-user-card-local-focusout').hide();
                }, function() {// focusOut
                    $('#sp-pos-sales-user-card-local-focusin').hide();
                    $('#sp-pos-sales-user-card-local-focusout').fadeIn(700);
                }, function(id) {
                    var res = _api.call({
                        request: 'usercard-quick-search',
                        dto: JSON.stringify({
                            card: id
                        })
                    });
                    if (res.result.code !== '0') {
                        _view.message(res.result.txt);
                    } else {
                        _onSales(res.dto.key, res.dto.text);
                    }
                });

        }).on("pageshow", function(event, ui) {
            // open first tab
            $('.sp-pos-tab-button').get(0).click();
            _clear();
        });

        return _self;
    };

}(jQuery, this, this.document, JSON, this.org.savapage));

// @license-end
