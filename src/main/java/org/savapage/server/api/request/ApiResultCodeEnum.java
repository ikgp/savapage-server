/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.server.api.request;

/**
 *
 * @author Rijk Ravestein
 *
 */
public enum ApiResultCodeEnum {

    /**
     * .
     */
    OK("0"),

    /**
     * .
     */
    INFO("1"),

    /**
     * .
     */
    WARN("2"),

    /**
     * .
     */
    ERROR("3"),

    /**
     * .
     */
    UNAVAILABLE("5"),

    /**
     * .
     */
    UNAUTH("9");

    private final String value;

    ApiResultCodeEnum(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
