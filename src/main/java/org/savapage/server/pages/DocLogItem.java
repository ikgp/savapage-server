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
package org.savapage.server.pages;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.crypto.CryptoUser;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.PrintOutDao;
import org.savapage.core.dao.enums.DaoEnumHelper;
import org.savapage.core.dao.enums.DocLogProtocolEnum;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
import org.savapage.core.dao.enums.PrintInDeniedReasonEnum;
import org.savapage.core.dao.enums.PrintModeEnum;
import org.savapage.core.dao.enums.ReservedIppQueueEnum;
import org.savapage.core.dao.helpers.DocLogPagerReq;
import org.savapage.core.doc.store.DocStoreBranchEnum;
import org.savapage.core.doc.store.DocStoreTypeEnum;
import org.savapage.core.ipp.IppJobStateEnum;
import org.savapage.core.ipp.helpers.IppOptionMap;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.DocIn;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.DocOut;
import org.savapage.core.jpa.PdfOut;
import org.savapage.core.jpa.PrintIn;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.services.DocStoreService;
import org.savapage.core.services.QueueService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.PrintSupplierData;
import org.savapage.core.services.helpers.ThirdPartyEnum;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.JsonHelper;
import org.savapage.core.util.NumberUtil;
import org.savapage.server.WebApp;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DocLogItem {

    private Long docLogId;
    private ExternalSupplierEnum extSupplier;
    private ExternalSupplierStatusEnum extSupplierStatus;
    private PrintSupplierData printSupplierData;
    private String extId;
    private String extData;
    private String mimeType;

    private String userId;
    private String userName;

    private String header;
    private DocLogDao.Type docType;
    private Date createdDate;
    private Date completedDate;
    private String title;
    private String comment;
    private String deliveryProtocol;

    private String currencyCode;
    private BigDecimal cost;
    private BigDecimal costOriginal;
    private boolean refunded;

    private String humanReadableByteCount;
    private int totalPages;
    private int totalSheets;
    private PrintModeEnum printMode;
    private int copies;
    private Boolean drmRestricted;

    private String signature;
    private String destination;
    private Boolean letterhead;
    private Boolean duplex;
    private Integer numberUp;
    private Boolean grayscale;

    private String printOutPrinterName;

    private boolean pageRotate180;
    private boolean finishingPunch;
    private boolean finishingStaple;
    private boolean finishingFold;
    private boolean finishingBooklet;

    private boolean printArchive;
    private boolean printJournal;

    private Map<String, String> ippOptions;

    private IppOptionMap ippOptionMap;

    private Boolean collateCopies;
    private Boolean ecoPrint;
    private Boolean removeGraphics;

    private Integer jobId;
    private IppJobStateEnum jobState;
    private Boolean encrypted;
    private String paperSize;

    private String docInOriginatorIp;

    private Boolean printInPrinted;
    private PrintInDeniedReasonEnum printInDeniedReason;
    private ReservedIppQueueEnum printInReservedQueue;

    private String author;
    private String subject;
    private String keywords;
    private Boolean userPw;
    private Boolean ownerPw;

    private List<AccountTrx> transactions;

    public Long getDocLogId() {
        return docLogId;
    }

    public void setDocLogId(Long docLogId) {
        this.docLogId = docLogId;
    }

    public ExternalSupplierEnum getExtSupplier() {
        return extSupplier;
    }

    public void setExtSupplier(ExternalSupplierEnum extSupplier) {
        this.extSupplier = extSupplier;
    }

    /**
     * @return {@code true} if external supplier is present.
     */
    public boolean isExtSupplierPresent() {
        return this.extSupplier != null;
    }

    public ExternalSupplierStatusEnum getExtSupplierStatus() {
        return extSupplierStatus;
    }

    public void
            setExtSupplierStatus(ExternalSupplierStatusEnum extSupplierStatus) {
        this.extSupplierStatus = extSupplierStatus;
    }

    /**
     * @return {@code null} if not present.
     */
    public ThirdPartyEnum getExtPrintManager() {
        if (this.printSupplierData == null) {
            return null;
        }
        return this.printSupplierData.getClient();
    }

    public PrintSupplierData getPrintSupplierData() {
        return printSupplierData;
    }

    public void setPrintSupplierData(PrintSupplierData printSupplierData) {
        this.printSupplierData = printSupplierData;
    }

    public String getExtId() {
        return extId;
    }

    public void setExtId(String extId) {
        this.extId = extId;
    }

    public String getExtData() {
        return extData;
    }

    public void setExtData(String extData) {
        this.extData = extData;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public List<AccountTrx> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<AccountTrx> transactions) {
        this.transactions = transactions;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public DocLogDao.Type getDocType() {
        return docType;
    }

    public void setDocType(DocLogDao.Type docType) {
        this.docType = docType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(Date completedDate) {
        this.completedDate = completedDate;
    }

    public String getHumanReadableByteCount() {
        return humanReadableByteCount;
    }

    public void setHumanReadableByteCount(String humanReadableByteCount) {
        this.humanReadableByteCount = humanReadableByteCount;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    /**
     *
     *
     */
    public static abstract class AbstractQuery {

        protected abstract String getExtraWhere(DocLogPagerReq req);

        protected abstract String getExtraJoin();

        protected abstract void setExtraParms(final Query query,
                DocLogPagerReq req);

        /**
         *
         * @param em
         * @param count
         * @param userId
         * @param req
         * @return
         */
        private String getSelectString(final EntityManager em,
                final boolean count, final Long userId, DocLogPagerReq req) {

            final StringBuilder jpql = new StringBuilder();

            jpql.append(getSelectCommon(count, req));

            //
            final String join = getExtraJoin();
            if (join != null) {
                jpql.append(" ").append(join);
            }

            //
            final String whereCommon = getWhereCommon(userId, req);
            if (whereCommon != null) {
                jpql.append(" WHERE ").append(whereCommon);
            }

            //
            final String where = getExtraWhere(req);
            if (where != null) {
                if (whereCommon == null) {
                    jpql.append(" WHERE ");
                } else {
                    jpql.append(" AND ");
                }
                jpql.append(where);
            }

            return jpql.toString();
        }

        /**
         *
         * @param em
         * @param userId
         * @param req
         * @return
         */
        public long filteredCount(final EntityManager em, final Long userId,
                DocLogPagerReq req) {

            final String jpql = getSelectString(em, true, userId, req);

            final Query query = em.createQuery(jpql);

            final Date dayFrom = req.getSelect().dateFrom();
            final Date dayTo = req.getSelect().dateTo();
            final String titleText = req.getSelect().getDocName();
            final Long accountId = req.getSelect().getAccountId();

            setParmsCommon(query, userId, accountId, dayFrom, dayTo, titleText);
            setExtraParms(query, req);

            final Number countResult = (Number) query.getSingleResult();
            return countResult.longValue();
        }

        /**
         *
         * @param orderBy
         * @return
         */
        protected abstract String
                getOrderByField(final DocLogDao.FieldEnum orderBy);

        /**
         *
         * @param em
         * @param userId
         * @param req
         * @param locale
         * @return
         */
        @SuppressWarnings("unchecked")
        public List<DocLogItem> getListChunk(final EntityManager em,
                final Long userId, final DocLogPagerReq req,
                final Locale locale) {

            final PrintOutDao printOutDAO =
                    ServiceContext.getDaoContext().getPrintOutDao();

            final StringBuilder jpql = new StringBuilder();

            jpql.append(getSelectString(em, false, userId, req));

            /*
             *
             */
            final DocLogDao.FieldEnum orderBy = req.getSort().getSortField();
            final boolean sortAscending = req.getSort().getAscending();

            String orderField = null;

            switch (orderBy) {
            case DOC_NAME:
                orderField = "D.title";
                break;
            case CREATE_DATE:
                orderField = "D.createdDate";
                break;
            default:
                orderField = getOrderByField(orderBy);
                break;
            }

            if (orderField != null) {

                jpql.append(" ORDER BY ").append(orderField);

                if (!sortAscending) {
                    jpql.append(" DESC");
                }

                jpql.append(", D.id DESC");
            }

            final Query query = em.createQuery(jpql.toString());

            final Date dayFrom = req.getSelect().dateFrom();
            final Date dayTo = req.getSelect().dateTo();
            final String titleText = req.getSelect().getDocName();
            final Long accountId = req.getSelect().getAccountId();

            setParmsCommon(query, userId, accountId, dayFrom, dayTo, titleText);
            setExtraParms(query, req);

            Integer startPosition = req.calcStartPosition();
            Integer maxResults = req.getMaxResults();

            if (startPosition != null) {
                query.setFirstResult(startPosition);
            }
            if (maxResults != null) {
                query.setMaxResults(maxResults);
            }

            final QueueService queueService =
                    ServiceContext.getServiceFactory().getQueueService();

            final DocStoreService docStoreService =
                    ServiceContext.getServiceFactory().getDocStoreService();

            final List<DocLogItem> list = new ArrayList<>();

            final boolean isPrintOutArchiveEnabled = docStoreService.isEnabled(
                    DocStoreTypeEnum.ARCHIVE, DocStoreBranchEnum.OUT_PRINT);

            final boolean isPrintOutJournalEnabled = docStoreService.isEnabled(
                    DocStoreTypeEnum.JOURNAL, DocStoreBranchEnum.OUT_PRINT);

            final boolean isPrintInArchiveEnabled = docStoreService.isEnabled(
                    DocStoreTypeEnum.ARCHIVE, DocStoreBranchEnum.IN_PRINT);

            final boolean isPrintInJournalEnabled = docStoreService.isEnabled(
                    DocStoreTypeEnum.JOURNAL, DocStoreBranchEnum.IN_PRINT);

            for (final DocLog docLog : ((List<DocLog>) query.getResultList())) {

                DocLogItem log = new DocLogItem();

                log.setDocLogId(docLog.getId());
                log.setExtSupplier(DaoEnumHelper.getExtSupplier(docLog));
                log.setExtSupplierStatus(
                        DaoEnumHelper.getExtSupplierStatus(docLog));
                log.setExtId(docLog.getExternalId());
                log.setExtData(docLog.getExternalData());
                log.setMimeType(docLog.getMimetype());

                log.setUserId(docLog.getUser().getUserId());
                log.setUserName(docLog.getUser().getFullName());
                log.setTitle(docLog.getTitle());
                log.setComment(docLog.getLogComment());
                log.setDeliveryProtocol(docLog.getDeliveryProtocol());
                log.setDrmRestricted(docLog.getDrmRestricted());
                log.setCreatedDate(docLog.getCreatedDate());
                log.setCost(docLog.getCost());
                log.setCostOriginal(docLog.getCostOriginal());
                log.setRefunded(docLog.getRefunded());

                if (docLog.getNumberOfPages() == null) {
                    log.setTotalPages(0);
                } else {
                    log.setTotalPages(docLog.getNumberOfPages());
                }

                if (docLog.getTransactions() == null) {
                    log.setTransactions(new ArrayList<AccountTrx>());
                } else {
                    log.setTransactions(docLog.getTransactions());
                }

                if (log.getTransactions().isEmpty()) {
                    log.setCurrencyCode(ConfigManager.getAppCurrencyCode());
                } else {
                    log.setCurrencyCode(
                            log.getTransactions().get(0).getCurrencyCode());
                }

                if (docLog.getNumberOfBytes() != null
                        && docLog.getNumberOfBytes().longValue() > 0) {
                    log.setHumanReadableByteCount(
                            NumberUtil.humanReadableByteCountSI(locale,
                                    docLog.getNumberOfBytes()));
                }

                final DocIn docIn = docLog.getDocIn();
                final DocOut docOut = docLog.getDocOut();

                log.setTotalSheets(0);
                log.setCopies(1);

                if (docIn != null) {

                    log.setDocInOriginatorIp(docIn.getOriginatorIp());

                    final PrintIn printIn = docIn.getPrintIn();
                    log.setDocType(DocLogDao.Type.IN);

                    if (printIn != null) {

                        ReservedIppQueueEnum reservedQueue =
                                queueService.getReservedQueue(
                                        printIn.getQueue().getUrlPath());

                        if (reservedQueue == null) {
                            reservedQueue = ReservedIppQueueEnum.IPP_PRINT;
                        }

                        if (reservedQueue == ReservedIppQueueEnum.IPP_PRINT) {
                            final StringBuilder header = new StringBuilder();
                            header.append(WebApp.MOUNT_PATH_PRINTERS);
                            if (!printIn.getQueue().getUrlPath()
                                    .equals(ReservedIppQueueEnum.IPP_PRINT
                                            .getUrlPath())) {
                                header.append("/");
                            }
                            header.append(printIn.getQueue().getUrlPath());
                            log.setHeader(header.toString());
                        } else {
                            log.setHeader(reservedQueue.getUiText());
                        }

                        log.setPaperSize(printIn.getPaperSize());

                        log.setPrintInReservedQueue(reservedQueue);
                        log.setPrintInPrinted(printIn.getPrinted());
                        log.setPrintInDeniedReason(PrintInDeniedReasonEnum
                                .parseDbValue(printIn.getDeniedReason()));

                        log.setPrintArchive(isPrintInArchiveEnabled
                                && docStoreService.isDocPresent(
                                        DocStoreTypeEnum.ARCHIVE,
                                        DocStoreBranchEnum.IN_PRINT, docLog));

                        if (!log.isPrintArchive()) {
                            log.setPrintJournal(isPrintInJournalEnabled
                                    && docStoreService.isDocPresent(
                                            DocStoreTypeEnum.JOURNAL,
                                            DocStoreBranchEnum.IN_PRINT,
                                            docLog));
                        }

                    } else {
                        log.setHeader("???");
                    }

                } else if (docOut != null) {

                    log.setHeader(docOut.getDestination());
                    log.setSignature(docOut.getSignature());
                    log.setDestination(docOut.getDestination());

                    log.setLetterhead(docOut.getLetterhead() != null
                            && docOut.getLetterhead().booleanValue());

                    log.setEcoPrint(docOut.getEcoPrint());
                    log.setRemoveGraphics(docOut.getRemoveGraphics());

                    final PrintOut printOut = docOut.getPrintOut();
                    final PdfOut pdfOut = docOut.getPdfOut();

                    if (printOut != null) {

                        log.setDocType(DocLogDao.Type.PRINT);
                        log.setHeader(printOut.getPrinter().getDisplayName());

                        log.setPrintOutPrinterName(
                                printOut.getPrinter().getPrinterName());

                        if (docLog.getExternalData() != null) {
                            log.setPrintSupplierData(PrintSupplierData
                                    .createFromData(docLog.getExternalData()));
                        }

                        log.setDuplex(printOut.getDuplex());
                        log.setGrayscale(printOut.getGrayscale());
                        log.setCollateCopies(printOut.getCollateCopies());

                        log.setPaperSize(printOut.getPaperSize());
                        log.setJobId(printOut.getCupsJobId());
                        log.setJobState(printOutDAO.getIppJobState(printOut));
                        log.setTotalSheets(printOut.getNumberOfSheets());
                        log.setCopies(printOut.getNumberOfCopies());

                        log.setPrintMode(
                                PrintModeEnum.valueOf(printOut.getPrintMode()));

                        if (printOut.getCupsCompletedTime() != null) {
                            log.setCompletedDate(
                                    new Date(printOut.getCupsCompletedTime()
                                            * DateUtil.DURATION_MSEC_SECOND));
                        }

                        if (NumberUtils.isDigits(printOut.getCupsNumberUp())) {
                            log.setNumberUp(Integer
                                    .valueOf(printOut.getCupsNumberUp()));
                        }

                        final Map<String, String> ippOptions =
                                JsonHelper.createStringMapOrNull(
                                        printOut.getIppOptions());

                        if (ippOptions != null) {

                            log.setIppOptions(ippOptions);

                            final IppOptionMap optionMap =
                                    new IppOptionMap(ippOptions);
                            log.setIppOptionMap(optionMap);

                            log.setPageRotate180(optionMap.hasPageRotate180());
                            log.setFinishingBooklet(
                                    optionMap.hasFinishingBooklet());
                            log.setFinishingFold(optionMap.hasFinishingFold());
                            log.setFinishingPunch(
                                    optionMap.hasFinishingPunch());
                            log.setFinishingStaple(
                                    optionMap.hasFinishingStaple());
                        }

                        log.setPrintArchive(isPrintOutArchiveEnabled
                                && docStoreService.isDocPresent(
                                        DocStoreTypeEnum.ARCHIVE,
                                        DocStoreBranchEnum.OUT_PRINT, docLog));

                        if (!log.isPrintArchive()) {
                            log.setPrintJournal(isPrintOutJournalEnabled
                                    && docStoreService.isDocPresent(
                                            DocStoreTypeEnum.JOURNAL,
                                            DocStoreBranchEnum.OUT_PRINT,
                                            docLog));
                        }

                    } else if (pdfOut != null) {

                        log.setDocType(DocLogDao.Type.PDF);
                        log.setHeader("PDF");

                        log.setAuthor(pdfOut.getAuthor());
                        log.setSubject(pdfOut.getSubject());
                        log.setKeywords(pdfOut.getKeywords());
                        log.setEncrypted(pdfOut.getEncrypted());
                        log.setOwnerPw(StringUtils
                                .isNotBlank(pdfOut.getPasswordOwner()));
                        log.setUserPw(StringUtils
                                .isNotBlank(pdfOut.getPasswordUser()));
                    }
                } else {
                    if (docLog.getDeliveryProtocol() != null
                            && docLog.getDeliveryProtocol().equals(
                                    DocLogProtocolEnum.IPP.getDbName())) {
                        log.setDocType(DocLogDao.Type.IN);
                    }
                }

                list.add(log);
            }
            return list;

        }

        /**
         *
         */
        protected final String getSelectCommon(final boolean count,
                final DocLogPagerReq req) {

            final StringBuilder jpql = new StringBuilder();

            jpql.append("SELECT ");

            if (count) {
                jpql.append("COUNT(D.id)");
            } else {
                jpql.append("D");
            }

            if (req.getSelect().getAccountId() != null) {
                jpql.append(" FROM AccountTrx TRX JOIN TRX.docLog D");

            } else {
                jpql.append(" FROM DocLog D JOIN D.user U");
            }

            return jpql.toString();
        }

        /**
         *
         * @param userId
         * @param req
         * @return
         */
        protected final String getWhereCommon(final Long userId,
                final DocLogPagerReq req) {

            final Date dayFrom = req.getSelect().dateFrom();
            final Date dayTo = req.getSelect().dateTo();
            final String titleText = req.getSelect().getDocName();

            int nWhere = 0;

            final StringBuilder where = new StringBuilder();

            if (req.getSelect().getAccountId() != null) {
                if (nWhere > 0) {
                    where.append(" AND ");
                }
                nWhere++;
                where.append("TRX.id = :accountId");
            }

            if (userId != null) {
                if (nWhere > 0) {
                    where.append(" AND ");
                }
                nWhere++;
                where.append("U.id = :userId");
            }

            if (dayFrom != null) {
                if (nWhere > 0) {
                    where.append(" AND ");
                }
                nWhere++;
                where.append("D.createdDay >= :dayFrom");
            }

            if (dayTo != null) {
                if (nWhere > 0) {
                    where.append(" AND ");
                }
                nWhere++;
                where.append("D.createdDay <= :dayTo");
            }

            if (titleText != null) {
                if (nWhere > 0) {
                    where.append(" AND ");
                }
                nWhere++;
                where.append("lower(D.title) like :titleText");
            }

            if (nWhere == 0) {
                return null;
            }

            return where.toString();
        }

        /**
         *
         * @param query
         * @param userId
         * @param accountId
         * @param dayFrom
         * @param dayTo
         * @param titleText
         */
        protected final void setParmsCommon(final Query query,
                final Long userId, final Long accountId, final Date dayFrom,
                final Date dayTo, final String titleText) {

            if (userId != null) {
                query.setParameter("userId", userId);
            }
            if (accountId != null) {
                query.setParameter("accountId", accountId);
            }
            if (dayFrom != null) {
                query.setParameter("dayFrom", dayFrom);
            }
            if (dayTo != null) {
                query.setParameter("dayTo", dayTo);
            }
            if (titleText != null) {
                query.setParameter("titleText",
                        String.format("%%%s%%", titleText.toLowerCase()));
            }
        }
    }

    /**
     *
     *
     */
    private static class QAll extends AbstractQuery {

        @Override
        public final String getExtraJoin() {
            return null;
        }

        @Override
        public final String getExtraWhere(final DocLogPagerReq req) {
            return null;
        }

        @Override
        protected void setExtraParms(final Query query,
                final DocLogPagerReq req) {
            // no code intended
        }

        @Override
        protected String getOrderByField(final DocLogDao.FieldEnum orderBy) {
            return null;
        }
    };

    /**
     *
     */
    private static class QIn extends AbstractQuery {

        @Override
        protected String getExtraWhere(final DocLogPagerReq req) {
            String jpql = null;
            Long id = req.getSelect().getQueueId();
            if (id != null && id > 0) {
                jpql = "Q.id = :queue_id";
            }
            return jpql;
        }

        @Override
        protected String getExtraJoin() {
            return "JOIN D.docIn I JOIN I.printIn P JOIN P.queue Q";
        }

        @Override
        protected void setExtraParms(final Query query,
                final DocLogPagerReq req) {
            Long id = req.getSelect().getQueueId();
            if (id != null && id > 0) {
                query.setParameter("queue_id", id);
            }
        }

        @Override
        protected String getOrderByField(final DocLogDao.FieldEnum orderBy) {

            if (orderBy == DocLogDao.FieldEnum.QUEUE) {
                return "Q.urlPath";
            } else {
                return null;
            }
        }
    }

    /**
     *
     */
    private static class QOut extends AbstractQuery {

        @Override
        protected String getExtraWhere(final DocLogPagerReq req) {

            final String selSignature = req.getSelect().getSignature();
            final String selDestination = req.getSelect().getDestination();
            final Boolean selLetterhead = req.getSelect().getLetterhead();

            int nWhere = 0;
            final StringBuilder where = new StringBuilder();

            if (selSignature != null) {
                if (nWhere > 0) {
                    where.append(" AND ");
                }
                nWhere++;
                where.append("lower(O.signature) like :signature");
            }

            if (selDestination != null) {
                if (nWhere > 0) {
                    where.append(" AND ");
                }
                nWhere++;
                where.append("lower(O.destination) like :destination");
            }

            if (selLetterhead != null) {
                if (nWhere > 0) {
                    where.append(" AND ");
                }
                nWhere++;
                where.append("O.letterhead = :letterhead");
            }

            if (nWhere == 0) {
                return null;
            }

            return where.toString();
        }

        @Override
        protected String getExtraJoin() {
            return "JOIN D.docOut O";
        }

        @Override
        protected void setExtraParms(final Query query,
                final DocLogPagerReq req) {

            final String selSignature = req.getSelect().getSignature();
            final String selDestination = req.getSelect().getDestination();
            final Boolean selLetterhead = req.getSelect().getLetterhead();

            if (selSignature != null) {
                query.setParameter("signature",
                        String.format("%%%s%%", selSignature.toLowerCase()));
            }
            if (selDestination != null) {
                query.setParameter("destination",
                        String.format("%%%s%%", selDestination.toLowerCase()));
            }
            if (selLetterhead != null) {
                query.setParameter("letterhead", selLetterhead);
            }
        }

        @Override
        protected String getOrderByField(final DocLogDao.FieldEnum orderBy) {
            return null;
        }
    }

    /**
     *
     */
    private static class QPdf extends QOut {
        @Override
        protected String getExtraWhere(final DocLogPagerReq req) {

            int nWhere = 0;

            final StringBuilder where = new StringBuilder();

            //
            final String extraWhere = super.getExtraWhere(req);
            if (extraWhere != null) {
                where.append(extraWhere);
                nWhere++;
            }

            //
            final String selAuthor = req.getSelect().getAuthor();
            final String selSubject = req.getSelect().getSubject();
            final String selKeywords = req.getSelect().getKeywords();
            final String selUserpw = req.getSelect().getUserPw();
            final String selOwnerpw = req.getSelect().getOwnerPw();
            final Boolean selEncrypted = req.getSelect().getEncrypted();

            if (selAuthor != null) {
                if (nWhere > 0) {
                    where.append(" AND ");
                }
                nWhere++;
                where.append("lower(F.author) like :author");
            }
            if (selSubject != null) {
                if (nWhere > 0) {
                    where.append(" AND ");
                }
                nWhere++;
                where.append("lower(F.subject) like :subject");
            }
            if (selKeywords != null) {
                if (nWhere > 0) {
                    where.append(" AND ");
                }
                nWhere++;
                where.append("lower(F.keywords) like :keywords");
            }
            if (selUserpw != null) {
                if (nWhere > 0) {
                    where.append(" AND ");
                }
                nWhere++;
                where.append("F.passwordUser = :userpw");
            }
            if (selOwnerpw != null) {
                if (nWhere > 0) {
                    where.append(" AND ");
                }
                nWhere++;
                where.append("F.passwordOwner = :ownerpw");
            }
            if (selEncrypted != null) {
                if (nWhere > 0) {
                    where.append(" AND ");
                }
                nWhere++;
                where.append("F.encrypted = :encrypted");
            }

            if (nWhere == 0) {
                return null;
            }
            return where.toString();
        }

        @Override
        protected String getExtraJoin() {
            return "JOIN D.docOut O JOIN O.pdfOut F";
        }

        @Override
        protected void setExtraParms(final Query query,
                final DocLogPagerReq req) {

            super.setExtraParms(query, req);

            final String selAuthor = req.getSelect().getAuthor();
            final String selSubject = req.getSelect().getSubject();
            final String selKeywords = req.getSelect().getKeywords();
            final String selUserpw = req.getSelect().getUserPw();
            final String selOwnerpw = req.getSelect().getOwnerPw();
            final Boolean selEncrypted = req.getSelect().getEncrypted();

            if (selAuthor != null) {
                query.setParameter("author",
                        String.format("%%%s%%", selAuthor.toLowerCase()));
            }
            if (selSubject != null) {
                query.setParameter("subject",
                        String.format("%%%s%%", selSubject.toLowerCase()));
            }
            if (selKeywords != null) {
                query.setParameter("keywords",
                        String.format("%%%s%%", selKeywords.toLowerCase()));
            }
            if (selUserpw != null) {
                query.setParameter("userpw", CryptoUser.encrypt(selUserpw));
            }
            if (selOwnerpw != null) {
                query.setParameter("ownerpw", CryptoUser.encrypt(selOwnerpw));
            }
            if (selEncrypted != null) {
                query.setParameter("encrypted", selEncrypted);
            }
        }

        @Override
        protected String getOrderByField(final DocLogDao.FieldEnum orderBy) {
            return null;
        }
    }

    /**
     *
     */
    private static class QPrint extends QOut {

        @Override
        protected String getExtraWhere(final DocLogPagerReq req) {

            int nWhere = 0;

            final StringBuilder where = new StringBuilder();

            //
            String extraWhere = super.getExtraWhere(req);
            if (extraWhere != null) {
                where.append(extraWhere);
                nWhere++;
            }

            //
            Long selId = req.getSelect().getPrinterId();
            final Boolean selDuplex = req.getSelect().getDuplex();
            final DocLogDao.JobState selState =
                    req.getSelect().getPrintOutState();

            if (selId != null && selId > 0) {
                if (nWhere > 0) {
                    where.append(" AND ");
                }
                nWhere++;
                where.append("S.id = :printer_id");
            }

            if (selDuplex != null) {
                if (nWhere > 0) {
                    where.append(" AND ");
                }
                nWhere++;
                where.append("P.duplex = :duplex");
            }

            //
            if (selState == DocLogDao.JobState.ACTIVE) {

                if (nWhere > 0) {
                    where.append(" AND ");
                }
                nWhere++;
                where.append("(P.cupsJobState <= ")
                        .append(IppJobStateEnum.IPP_JOB_STOPPED.asInt());

                where.append(" OR D.externalStatus IN(").append("\'")
                        .append(ExternalSupplierStatusEnum.PENDING.toString())
                        .append("\', \'")
                        .append(ExternalSupplierStatusEnum.PENDING_EXT
                                .toString())
                        .append("\'").append("))");

            } else if (selState == DocLogDao.JobState.UNFINISHED) {

                if (nWhere > 0) {
                    where.append(" AND ");
                }
                nWhere++;
                where.append("(P.cupsJobState = ")
                        .append(IppJobStateEnum.IPP_JOB_CANCELED.asInt())
                        .append(" OR P.cupsJobState = ")
                        .append(IppJobStateEnum.IPP_JOB_ABORTED.asInt());

                where.append(" OR D.externalStatus IN(").append("\'")
                        .append(ExternalSupplierStatusEnum.CANCELLED.toString())
                        .append("\', \'")
                        .append(ExternalSupplierStatusEnum.PENDING_CANCEL
                                .toString())
                        .append("\', \'")
                        .append(ExternalSupplierStatusEnum.EXPIRED.toString())
                        .append("\', \'")
                        .append(ExternalSupplierStatusEnum.ERROR.toString())
                        .append("\'))");

            } else if (selState == DocLogDao.JobState.COMPLETED) {

                if (nWhere > 0) {
                    where.append(" AND ");
                }
                nWhere++;
                where.append("P.cupsJobState = ")
                        .append(IppJobStateEnum.IPP_JOB_COMPLETED.asInt());

                where.append(" AND (D.externalStatus IS NULL "
                        + "OR D.externalStatus IN(").append("\'")
                        .append(ExternalSupplierStatusEnum.COMPLETED.toString())
                        .append("\', \'")
                        .append(ExternalSupplierStatusEnum.PENDING_COMPLETE
                                .toString())
                        .append("\'))");
            }
            //
            if (nWhere == 0) {
                return null;
            }
            return where.toString();
        }

        @Override
        protected String getExtraJoin() {
            return "JOIN D.docOut O JOIN O.printOut P JOIN P.printer S";
        }

        @Override
        protected void setExtraParms(final Query query,
                final DocLogPagerReq req) {

            super.setExtraParms(query, req);

            Long selId = req.getSelect().getPrinterId();
            final Boolean selDuplex = req.getSelect().getDuplex();

            if (selId != null && selId > 0) {
                query.setParameter("printer_id", selId);
            }

            if (selDuplex != null) {
                query.setParameter("duplex", selDuplex);
            }
        }

        @Override
        protected String getOrderByField(final DocLogDao.FieldEnum orderBy) {
            if (orderBy == DocLogDao.FieldEnum.PRINTER) {
                return "S.displayName";
            } else {
                return null;
            }
        }

    }

    /**
     * .
     */
    private static class QTicket extends QPrint {

        @Override
        protected String getExtraWhere(final DocLogPagerReq req) {

            int nWhere = 0;

            final StringBuilder where = new StringBuilder();

            //
            final String extraWhere = super.getExtraWhere(req);
            if (extraWhere != null) {
                where.append(extraWhere);
                nWhere++;
            }

            //
            if (nWhere > 0) {
                where.append(" AND ");
            }
            nWhere++;

            where.append("P.printMode IN (:printModes)");

            //
            if (req.getSelect().getTicketNumber() != null) {
                if (nWhere > 0) {
                    where.append(" AND ");
                }
                nWhere++;

                where.append(
                        "lower(D.externalId) like :containingExternalIdText");
            }
            //
            return where.toString();
        }

        @Override
        protected void setExtraParms(final Query query,
                final DocLogPagerReq req) {

            super.setExtraParms(query, req);

            List<String> names = Arrays.asList(PrintModeEnum.TICKET.toString(),
                    PrintModeEnum.TICKET_C.toString(),
                    PrintModeEnum.TICKET_E.toString());

            query.setParameter("printModes", names);

            final String ticket = req.getSelect().getTicketNumber();
            if (ticket != null) {
                query.setParameter("containingExternalIdText",
                        String.format("%%%s%%", ticket.toLowerCase()));
            }
        }
    }

    /** */
    private DocLogItem() {
    }

    /**
     *
     * @param em
     * @param userId
     * @param req
     * @return
     */
    public static AbstractQuery createQuery(final DocLogDao.Type docType) {

        switch (docType) {
        case ALL:
            return new QAll();
        case IN:
            return new QIn();
        case OUT:
            return new QOut();
        case PDF:
            return new QPdf();
        case PRINT:
            return new QPrint();
        case TICKET:
            return new QTicket();
        default:
            throw new SpException(
                    String.format("Unknown doctype [%s]", docType.toString()));
        }
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Boolean getLetterhead() {
        return letterhead;
    }

    public void setLetterhead(Boolean letterhead) {
        this.letterhead = letterhead;
    }

    public Boolean getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getPrintOutPrinterName() {
        return printOutPrinterName;
    }

    public void setPrintOutPrinterName(String printOutPrinterName) {
        this.printOutPrinterName = printOutPrinterName;
    }

    public Boolean getDuplex() {
        return duplex;
    }

    public void setDuplex(Boolean duplex) {
        this.duplex = duplex;
    }

    public Integer getNumberUp() {
        return numberUp;
    }

    public void setNumberUp(Integer numberUp) {
        this.numberUp = numberUp;
    }

    public Boolean getGrayscale() {
        return grayscale;
    }

    public void setGrayscale(Boolean grayscale) {
        this.grayscale = grayscale;
    }

    public boolean isPageRotate180() {
        return pageRotate180;
    }

    public void setPageRotate180(boolean pageRotate180) {
        this.pageRotate180 = pageRotate180;
    }

    public boolean isFinishingPunch() {
        return finishingPunch;
    }

    public void setFinishingPunch(boolean finishingPunch) {
        this.finishingPunch = finishingPunch;
    }

    public boolean isFinishingStaple() {
        return finishingStaple;
    }

    public void setFinishingStaple(boolean finishingStaple) {
        this.finishingStaple = finishingStaple;
    }

    public boolean isFinishingFold() {
        return finishingFold;
    }

    public void setFinishingFold(boolean finishingFold) {
        this.finishingFold = finishingFold;
    }

    public boolean isFinishingBooklet() {
        return finishingBooklet;
    }

    public void setFinishingBooklet(boolean finishingBooklet) {
        this.finishingBooklet = finishingBooklet;
    }

    public boolean isPrintArchive() {
        return printArchive;
    }

    public void setPrintArchive(boolean printArchive) {
        this.printArchive = printArchive;
    }

    public boolean isPrintJournal() {
        return printJournal;
    }

    public void setPrintJournal(boolean printJournal) {
        this.printJournal = printJournal;
    }

    public Map<String, String> getIppOptions() {
        return ippOptions;
    }

    public void setIppOptions(Map<String, String> ippOptions) {
        this.ippOptions = ippOptions;
    }

    public IppOptionMap getIppOptionMap() {
        return ippOptionMap;
    }

    public void setIppOptionMap(IppOptionMap ippOptionMap) {
        this.ippOptionMap = ippOptionMap;
    }

    public Boolean getCollateCopies() {
        return collateCopies;
    }

    public void setCollateCopies(Boolean collateCopies) {
        this.collateCopies = collateCopies;
    }

    public Boolean getEcoPrint() {
        return ecoPrint;
    }

    public void setEcoPrint(Boolean ecoPrint) {
        this.ecoPrint = ecoPrint;
    }

    public Boolean getRemoveGraphics() {
        return removeGraphics;
    }

    public void setRemoveGraphics(Boolean removeGraphics) {
        this.removeGraphics = removeGraphics;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public Boolean getUserPw() {
        return userPw;
    }

    public void setUserPw(Boolean userPw) {
        this.userPw = userPw;
    }

    public Boolean getOwnerPw() {
        return ownerPw;
    }

    public void setOwnerPw(Boolean ownerPw) {
        this.ownerPw = ownerPw;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPaperSize() {
        return paperSize;
    }

    public void setPaperSize(String paperSize) {
        this.paperSize = paperSize;
    }

    public Integer getJobId() {
        return jobId;
    }

    public void setJobId(Integer jobId) {
        this.jobId = jobId;
    }

    public IppJobStateEnum getJobState() {
        return jobState;
    }

    public void setJobState(IppJobStateEnum jobState) {
        this.jobState = jobState;
    }

    public Boolean getDrmRestricted() {
        return drmRestricted;
    }

    public void setDrmRestricted(Boolean drmRestricted) {
        this.drmRestricted = drmRestricted;
    }

    public Boolean getPrintInPrinted() {
        return printInPrinted;
    }

    public void setPrintInPrinted(Boolean printInPrinted) {
        this.printInPrinted = printInPrinted;
    }

    public String getDocInOriginatorIp() {
        return docInOriginatorIp;
    }

    public void setDocInOriginatorIp(String docInOriginatorIp) {
        this.docInOriginatorIp = docInOriginatorIp;
    }

    public ReservedIppQueueEnum getPrintInReservedQueue() {
        return printInReservedQueue;
    }

    public void
            setPrintInReservedQueue(ReservedIppQueueEnum printInReservedQueue) {
        this.printInReservedQueue = printInReservedQueue;
    }

    public PrintInDeniedReasonEnum getPrintInDeniedReason() {
        return printInDeniedReason;
    }

    public void setPrintInDeniedReason(
            PrintInDeniedReasonEnum printInDeniedReason) {
        this.printInDeniedReason = printInDeniedReason;
    }

    public String getDeliveryProtocol() {
        return deliveryProtocol;
    }

    public void setDeliveryProtocol(String deliveryProtocol) {
        this.deliveryProtocol = deliveryProtocol;
    }

    public int getTotalSheets() {
        return totalSheets;
    }

    public void setTotalSheets(int totalSheets) {
        this.totalSheets = totalSheets;
    }

    public int getCopies() {
        return copies;
    }

    public void setCopies(int copies) {
        this.copies = copies;
    }

    public PrintModeEnum getPrintMode() {
        return printMode;
    }

    public void setPrintMode(PrintModeEnum printMode) {
        this.printMode = printMode;
    }

    public boolean isJobTicket() {
        return this.printMode != null && this.printMode.isJobTicket();
    }

    /**
     * @return {@code true} is no cost is charged (optionally after a refund).
     */
    public boolean isZeroCost() {
        return this.getCost().compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * @return {@code true} is no original cost is charged.
     */
    public boolean isZeroCostOriginal() {
        return this.getCostOriginal().compareTo(BigDecimal.ZERO) == 0;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public BigDecimal getCostOriginal() {
        return costOriginal;
    }

    public void setCostOriginal(BigDecimal costOriginal) {
        this.costOriginal = costOriginal;
    }

    public boolean isRefunded() {
        return refunded;
    }

    public void setRefunded(boolean refunded) {
        this.refunded = refunded;
    }

}
