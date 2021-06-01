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
package org.savapage.server.auth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.savapage.core.config.WebAppTypeEnum;

/**
 * Singleton manager of WebApp User Authentications Tokens.
 * <p>
 * A separate cache dictionary for each Web App context is maintained (User,
 * Admin, POS, JobTicket, MailTicket, PrintSite).
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class WebAppUserAuthManager {

    /**
     * Number of indexes.
     */
    private static final int IDX_CONTEXT_COUNT = 6;

    /** */
    private static final int IDX_CONTEXT_USER = IDX_CONTEXT_COUNT - 6;
    /** */
    private static final int IDX_CONTEXT_ADMIN = IDX_CONTEXT_COUNT - 5;
    /** */
    private static final int IDX_CONTEXT_POS = IDX_CONTEXT_COUNT - 4;
    /** */
    private static final int IDX_CONTEXT_JOBTICKET = IDX_CONTEXT_COUNT - 3;
    /** */
    private static final int IDX_CONTEXT_MAILTICKET = IDX_CONTEXT_COUNT - 2;
    /** */
    private static final int IDX_CONTEXT_PRINTSITE = IDX_CONTEXT_COUNT - 1;

    /** */
    private final Object mutexDict = new Object();

    /**
     * WebApp User/Admin Context Dictionary of {@link UserAuthToken} objects
     * with 'token' key.
     */
    private final ArrayList<Map<String, UserAuthToken>> dictTokenAuthToken =
            new ArrayList<Map<String, UserAuthToken>>(IDX_CONTEXT_COUNT);

    /**
     * WebApp User Context Dictionary of {@link UserAuthToken} objects with
     * 'user' key.
     */
    private final ArrayList<Map<String, UserAuthToken>> dictUserAuthToken =
            new ArrayList<Map<String, UserAuthToken>>(IDX_CONTEXT_COUNT);

    /**
     *
     */
    private WebAppUserAuthManager() {

        for (int i = 0; i < IDX_CONTEXT_COUNT; i++) {
            dictTokenAuthToken.add(i, new HashMap<String, UserAuthToken>());
            dictUserAuthToken.add(i, new HashMap<String, UserAuthToken>());
        }
    }

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link WebAppUserAuthManager#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final WebAppUserAuthManager INSTANCE =
                new WebAppUserAuthManager();
    }

    /**
     * Gets the singleton instance.
     *
     * @return
     */
    public static WebAppUserAuthManager instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * @param webAppType
     *            Web App Type.
     * @return
     */
    private int getDictIndex(final WebAppTypeEnum webAppType) {

        switch (webAppType) {
        case ADMIN:
            return IDX_CONTEXT_ADMIN;
        case PRINTSITE:
            return IDX_CONTEXT_PRINTSITE;
        case JOBTICKETS:
            return IDX_CONTEXT_JOBTICKET;
        case MAILTICKETS:
            return IDX_CONTEXT_MAILTICKET;
        case POS:
            return IDX_CONTEXT_POS;
        case USER:
            return IDX_CONTEXT_USER;
        default:
            throw new IllegalArgumentException(
                    String.format("%s.%s is NOT supported.",
                            WebAppTypeEnum.class.getSimpleName(),
                            webAppType.toString()));
        }
    }

    /**
     * Gets the authentication token object from the token string.
     *
     * @param token
     *            The token string.
     * @param webAppType
     *            The {@link WebAppTypeEnum}.
     * @return {@code null} when token is NOT found.
     */
    public UserAuthToken getUserAuthToken(final String token,
            final WebAppTypeEnum webAppType) {
        if (webAppType == WebAppTypeEnum.UNDEFINED) {
            return null;
        }
        synchronized (this.mutexDict) {
            return dictTokenAuthToken.get(getDictIndex(webAppType)).get(token);
        }
    }

    /**
     * Gets the authentication token object of the user.
     *
     * @param user
     *            The user id.
     * @param webAppType
     *            The {@link WebAppTypeEnum}.
     * @return {@code null} when token is NOT found.
     */
    public UserAuthToken getAuthTokenOfUser(final String user,
            final WebAppTypeEnum webAppType) {

        if (webAppType == WebAppTypeEnum.UNDEFINED) {
            return null;
        }
        synchronized (this.mutexDict) {
            return dictUserAuthToken.get(getDictIndex(webAppType)).get(user);
        }
    }

    /**
     * Adds a new token to the cache, replacing an old token object of the same
     * user.
     *
     * @param token
     *            The token to add.
     * @param webAppType
     *            The {@link WebAppTypeEnum}.
     * @return the old token or {@code null} when no old token was replaced.
     */
    public UserAuthToken putUserAuthToken(final UserAuthToken token,
            final WebAppTypeEnum webAppType) {

        final int i = getDictIndex(webAppType);

        synchronized (this.mutexDict) {

            final UserAuthToken oldToken =
                    dictUserAuthToken.get(i).get(token.getUser());

            if (oldToken != null) {
                dictTokenAuthToken.get(i).remove(oldToken.getToken());
                dictUserAuthToken.get(i).remove(oldToken.getUser());
            }

            dictTokenAuthToken.get(i).put(token.getToken(), token);
            dictUserAuthToken.get(i).put(token.getUser(), token);

            return oldToken;
        }
    }

    /**
     * Removes a token from the cache.
     *
     * @param token
     *            The token.
     * @param webAppType
     *            The {@link WebAppTypeEnum}.
     * @return the removed token or {@code null} when not found.
     */
    public UserAuthToken removeUserAuthToken(final String token,
            final WebAppTypeEnum webAppType) {

        if (webAppType == WebAppTypeEnum.UNDEFINED) {
            return null;
        }

        synchronized (this.mutexDict) {
            final int i = getDictIndex(webAppType);

            final UserAuthToken oldToken =
                    dictTokenAuthToken.get(i).remove(token);

            if (oldToken != null) {
                dictTokenAuthToken.get(i).remove(oldToken.getToken());
                dictUserAuthToken.get(i).remove(oldToken.getUser());
            }

            return oldToken;
        }
    }

}
