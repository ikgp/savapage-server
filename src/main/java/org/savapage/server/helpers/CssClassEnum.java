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
package org.savapage.server.helpers;

/**
 * CSS class names.
 *
 * @author Rijk Ravestein
 *
 */
public enum CssClassEnum {

    /**  */
    SP_BTN_ABOUT("sp-btn-about"),
    /** */
    SP_BTN_ABOUT_ORG("sp-btn-about-org"),
    /** */
    SP_BTN_ABOUT_USER_ID("sp-btn-about-userid");

    /**
     * .
     */
    private final String clazz;

    /**
     * Constructor.
     *
     * @param value
     *            The CSS class.
     */
    CssClassEnum(final String value) {
        this.clazz = value;
    }

    public String clazz() {
        return this.clazz;
    }

}
