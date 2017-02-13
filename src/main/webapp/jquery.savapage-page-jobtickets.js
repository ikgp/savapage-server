/*! SavaPage jQuery Mobile Job Tickets Page | (c) 2011-2017 Datraverse B.V. | GNU Affero General Public License */

/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
 * Author: Rijk Ravestein.
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

( function($, window, document, JSON, _ns) {"use strict";

		// =========================================================================
		/**
		 * Constructor
		 */
		_ns.PageJobTickets = function(_i18n, _view, _model, _api) {

			var _page = new _ns.Page(_i18n, _view, "#page-jobtickets", "PageJobTickets")
			//
			, _self = _ns.derive(_page)
			//
			, _pnlDocLog = _ns.PanelDocLogBase, _pnlDocLogRefresh = true
			//
			, _countdownTimer, _countdownCounter = 1, _countdownPaused
			//
			, _MODE_PRINT = '0', _MODE_CANCEL = '1'
			//
			, _userKey, _expiryAsc = true
			//
			, _quickUserSearch = new _ns.QuickUserSearch(_view, _api)
			//
			, _refresh = function() {

				var html = _view.getUserPageHtml('OutboxAddin', {
					jobTickets : true,
					userKey : _userKey,
					expiryAsc : _expiryAsc
				});

				if (html) {
					$('#jobticket-list').html(html).enhanceWithin();
					$('.sparkline-printout-pie').sparkline('html', {
						type : 'pie',
						sliceColors : [_view.colorPrinter, _view.colorSheet]
					});
				}

				_view.enable($('.sp-jobtickets-all'), $('.sp-outbox-job-entry').length > 0);

				if (!_countdownPaused) {
					_countdownCounter = 1;
					_startCountdownTimer();
				}

				return false;
			}
			//
			, _stopCountdownTimer = function() {
				if (_countdownTimer) {
					window.clearTimeout(_countdownTimer);
					_countdownTimer = null;
				}
			}
			//
			, _startCountdownTimer = function() {
				var msecDelay = 3000, refreshMsec = 60 * 1000;
				_stopCountdownTimer();
				_countdownTimer = window.setInterval(function() {
					var widthPerc = 100 * msecDelay * _countdownCounter / refreshMsec;
					$('#sp-jobticket-countdown').width(widthPerc + '%');
					if (_countdownCounter++ === refreshMsec / msecDelay) {
						_countdownCounter = 1;
						_refresh();
					}
				}, msecDelay);
			}
			//
			, _getRedirectPrinterItem = function(inputRadio) {
				return inputRadio.closest('.sp-jobticket-redirect-printer-item');
			},
			//
			_getRedirectPrinterMediaSource = function(redirectPrinterItem) {
				return redirectPrinterItem.find('.sp-redirect-printer-media-source');
			}
			//
			, _onSelectUser = function(quickUserSelected) {
				$("#sp-jobticket-userid").val(quickUserSelected.text);
				_userKey = quickUserSelected.key;
				_refresh();
			}
			//
			, _onClearUser = function() {
				$("#sp-jobticket-userid").val('');
				_userKey = null;
				_refresh();
			}
			//
			, _onPrintPopup = function(jobFileName, positionTo, settle) {
				var html = _view.getPageHtml('JobTicketPrintAddIn', {
					jobFileName : jobFileName,
					settle : settle
				}) || 'error';

				$('#sp-jobticket-popup-addin').html(html);
				$('#sp-jobticket-popup').enhanceWithin().popup('open', {
					positionTo : positionTo
				});

				if (!settle) {
					_onRedirectPrinterRadio(_view.getRadioSelected('sp-jobticket-redirect-printer'));
				}
			}
			//
			, _onEditPopup = function(jobFileName, positionTo) {
				var html = _view.getPageHtml('JobTicketEditAddIn', {
					jobFileName : jobFileName
				}) || 'error';

				$('#sp-jobticket-popup-addin').html(html);
				$('#sp-jobticket-popup').enhanceWithin().popup('open', {
					positionTo : positionTo
				});
			}
			//
			, _cancelJob = function(jobFileName) {
				return _api.call({
					request : 'jobticket-delete',
					dto : JSON.stringify({
						jobFileName : jobFileName
					})
				});
			}
			//
			, _execJob = function(jobFileName, print, printerId, mediaSource) {
				return _api.call({
					request : 'jobticket-execute',
					dto : JSON.stringify({
						jobFileName : jobFileName,
						print : print,
						printerId : printerId,
						mediaSource : mediaSource
					})
				});
			}
			//
			, _onSaveJob = function(jobFileName) {
				var res, ippOptions = {};

				$('#sp-jobticket-popup').find('select').each(function() {
					ippOptions[$(this).attr('data-savapage')] = $(this).find(':selected').val();
				});

				res = _api.call({
					request : 'jobticket-save',
					dto : JSON.stringify({
						jobFileName : jobFileName,
						copies : $('#sp-jobticket-edit-copies').val(),
						ippOptions : ippOptions
					})
				});

				if (res.result.code === "0") {
					$('#sp-jobticket-popup').popup('close');
					_refresh();
				}
				//_view.showApiMsg(res);
				_view.message(res.result.txt);
			}
			//
			, _onExecJob = function(jobFileName, print) {
				var res, selPrinter = _view.getRadioSelected('sp-jobticket-redirect-printer'), mediaSource;
				if (print) {
					mediaSource = _getRedirectPrinterMediaSource(_getRedirectPrinterItem(selPrinter)).find(':selected').val();
				}
				res = _execJob(jobFileName, print, selPrinter.val(), mediaSource);

				if (res.result.code === "0") {
					$('#sp-jobticket-popup').popup('close');
					_refresh();
				}
				//_view.showApiMsg(res);
				_view.message(res.result.txt);
			}
			//
			, _onProcessAll = function(mode) {

				var logPfx = (mode === _MODE_PRINT ? 'Print' : 'Cancel')
				//
				, popup = (mode === _MODE_PRINT ? $('#sp-jobticket-popup-print-all') : $('#sp-jobticket-popup-cancel-all'))
				//
				, tickets = $('.sp-outbox-cancel-jobticket');

				popup.find('.ui-content:eq(0)').hide();
				popup.find('.ui-content:eq(1)').show();

				// Stalled async execution: long enough to show the busy message to user.
				window.setTimeout(function() {
					tickets.each(function() {

						var res, msg;

						if (mode === _MODE_PRINT) {
							res = _execJob($(this).attr('data-savapage'), true);
						} else {
							res = _cancelJob($(this).attr('data-savapage'));
						}

						if (res && res.result.code !== "0") {
							msg = res.result.txt || 'unknown';
							_ns.logger.warn(logPfx + ' Job Ticket error: ' + msg);
							popup.find('.sp-progress-all-error').append(msg + '</br>');
						}
					});
					_refresh();
					popup.popup('close');
				}, 1500);
			}
			//
			, _onRedirectPrinterRadio = function(inputRadio) {
				var item = _getRedirectPrinterItem(inputRadio), mediaSource = _getRedirectPrinterMediaSource(item);
				if (mediaSource.length > 0) {
					_view.visible($('.sp-redirect-printer-media-source'), false);
					_view.visible(mediaSource, true);
					$('.sp-jobticket-redirect-printer-item').attr('style', '');
					item.attr('style', 'border: 4px solid silver;');
				}
			}
			//
			;

			$(_self.id()).on('pagecreate', function(event) {

				var id = 'sp-jobticket-sort-dir';

				_view.checkRadio(id, _expiryAsc ? id + '-asc' : id + '-desc');

				$('#btn-jobtickets-refresh').click(function() {
					_refresh();
					return false;
				});

				$(this).on('click', '.sp-outbox-cancel-jobticket', null, function() {
					var res = _cancelJob($(this).attr('data-savapage'));
					if (res.result.code === "0") {
						_refresh();
					}
					_view.showApiMsg(res);

				}).on('click', '.sp-outbox-preview-job', null, function() {
					_api.download("pdf-outbox", null, $(this).attr('data-savapage'));
					return false;

				}).on('click', '.sp-outbox-preview-jobticket', null, function() {
					_api.download("pdf-jobticket", null, $(this).attr('data-savapage'));
					return false;

				}).on('click', '.sp-jobticket-print', null, function() {
					_onPrintPopup($(this).attr('data-savapage'), $(this));

				}).on('click', '.sp-jobticket-settle', null, function() {
					_onPrintPopup($(this).attr('data-savapage'), $(this), true);

				}).on('click', '.sp-jobticket-edit', null, function() {
					_onEditPopup($(this).attr('data-savapage'), $(this));

				}).on('change', "input[name='sp-jobticket-sort-dir']", null, function() {
					_expiryAsc = $(this).attr('id') === 'sp-jobticket-sort-dir-asc';
					_refresh();
					return false;

				}).on('click', '#btn-jobtickets-cancel-all', null, function() {
					var dlg = $('#sp-jobticket-popup-cancel-all');

					dlg.find('.ui-content:eq(1)').hide();
					dlg.find('.ui-content:eq(0)').show();

					dlg.popup('open', {
						positionTo : $(this)
					});
					$("#sp-jobticket-popup-cancel-all-btn-no").focus();

				}).on('click', '#btn-jobtickets-print-all', null, function() {
					var dlg = $('#sp-jobticket-popup-print-all');

					dlg.find('.ui-content:eq(1)').hide();
					dlg.find('.ui-content:eq(0)').show();

					dlg.popup('open', {
						positionTo : $(this)
					});
					$("#sp-jobticket-popup-print-all-btn-no").focus();

				}).on('click', "#btn-jobtickets-close", null, function() {
					if (_self.onClose) {
						return _self.onClose();
					}
					return true;

				}).on('click', "#sp-jobticket-popup-btn-print", null, function() {
					_onExecJob($(this).attr('data-savapage'), true);

				}).on('click', "#sp-jobticket-popup-btn-settle", null, function() {
					_onExecJob($(this).attr('data-savapage'), false);

				}).on('click', '#sp-jobticket-popup-cancel-all-btn-yes', null, function() {
					_onProcessAll(_MODE_CANCEL);

				}).on('click', '#sp-jobticket-popup-print-all-btn-yes', null, function() {
					_onProcessAll(_MODE_PRINT);

				}).on('change', "input[name='sp-jobticket-redirect-printer']", null, function() {
					_onRedirectPrinterRadio($(this));

				}).on('click', '#sp-jobticket-edit-popup-btn-cancel', null, function() {
					$('#sp-jobticket-popup').popup('close');

				}).on('click', '#sp-jobticket-popup-btn-cancel', null, function() {
					$('#sp-jobticket-popup').popup('close');

				}).on('click', '#sp-jobticket-edit-popup-btn-save', null, function() {
					_onSaveJob($(this).attr('data-savapage'));

				}).on('click', '#sp-btn-jobticket-countdown-pause', null, function() {
					if (_countdownTimer) {
						_stopCountdownTimer();
						_countdownPaused = true;
						_view.visible($(this), false);
						_view.visible($('#sp-btn-jobticket-countdown-play'), true);
					}
				}).on('click', '#sp-btn-jobticket-countdown-play', null, function() {
					if (!_countdownTimer) {
						_startCountdownTimer();
						_countdownPaused = false;
						_view.visible($(this), false);
						_view.visible($('#sp-btn-jobticket-countdown-pause'), true);
					}
				});

				_quickUserSearch.onCreate($(this), 'sp-jobticket-userid-filter', _onSelectUser, _onClearUser);

				//--------------------------------------------
				// Common Panel parameters.
				//--------------------------------------------

				_ns.PanelCommon.view = _view;

				_ns.PanelCommon.refreshPanelCommon = function(wClass, skipBeforeLoad, thePanel) {
					var jqId = thePanel.jqId
					//
					, data = thePanel.getInput(thePanel)
					//
					, jsonData = JSON.stringify(data)
					//
					;

					$.mobile.loading("show");
					$.ajax({
						type : "POST",
						async : true,
						url : '/pages/' + wClass + _ns.WebAppTypeUrlParm(),
						data : {
							user : _ns.PanelCommon.userId,
							data : jsonData
						}
					}).done(function(html) {
						$(jqId).html(html).enhanceWithin();

						// Hide the top divider with the title
						$(jqId + ' > ul > .ui-li-divider').hide();
						// Hide the document type selection
						_view.visible($('#sp-doclog-cat-type'), false);

						thePanel.onOutput(thePanel, undefined);
						thePanel.afterload(thePanel);

					}).fail(function() {
						_ns.PanelCommon.onDisconnected();
					}).always(function() {
						$.mobile.loading("hide");
					});
				};

				//
				_pnlDocLog.jqId = '#sp-jobtickets-tab-closed';
				_pnlDocLog.applyDefaultForTicket(_pnlDocLog);

				// Load page
				$('#sp-jobtickets-tab-closed').html(_view.getPageHtml('DocLogBase')).enhanceWithin();

				$(this).on('click', '#sp-jobtickets-tab-closed-button', null, function() {
					if (!_countdownPaused) {
						_stopCountdownTimer();
					}
					if (_pnlDocLogRefresh) {
						_pnlDocLog.refresh(_pnlDocLog);
					} else {
						_pnlDocLogRefresh = false;
						_pnlDocLog.page(_pnlDocLog, 1);
					}

				}).on('click', '#sp-jobtickets-tab-open-button', null, function() {
					if (!_countdownPaused) {
						_startCountdownTimer();
					}

				}).on('click', '#button-doclog-apply', null, function() {
					_pnlDocLog.page(_pnlDocLog, 1);
					return false;

				}).on('click', '#button-doclog-default', null, function() {
					_pnlDocLog.applyDefaultForTicket(_pnlDocLog);
					_pnlDocLog.m2v(_pnlDocLog);
					return false;

				}).on('change', "input[name='sp-doclog-select-type']", null, function() {
					_pnlDocLog.setVisibility(_pnlDocLog);
					return false;
				});

				$("#sp-a-content-button").click();

				$('#sp-jobticket-popup,#sp-jobticket-popup-cancel-all,#sp-jobticket-popup-print-all').popup({
					afteropen : function(event, ui) {
						if (!_countdownPaused) {
							_stopCountdownTimer();
						}
					},
					afterclose : function(event, ui) {
						if (!_countdownPaused) {
							_startCountdownTimer();
						}
					}
				});

			_view.visible($('#sp-btn-jobticket-countdown-play'), false);

			}).on("pageshow", function(event, ui) {
				$('#sp-jobtickets-tab-open-button').click();
				_countdownCounter = 1;
				_refresh();

			}).on("pagehide", function(event, ui) {
				_stopCountdownTimer();
			});

			return _self;
		};

	}(jQuery, this, this.document, JSON, this.org.savapage));
