/*
 * Copyright (C) 2020  Hugo JOBY
 *
 *  This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty ofnMERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNUnLesser General Public License v3 for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public v3 License along with this library; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package com.castsoftware.artemis.nlp.parser;

public class GoogleResult {

    private String title;
    private String content;
    private int numberResult;
    private boolean blacklisted;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getNumberResult() {
        return numberResult;
    }

    public void setNumberResult(int numberResult) {
        this.numberResult = numberResult;
    }

    public boolean isBlacklisted() {
        return blacklisted;
    }

    public void setBlacklisted(boolean blacklisted) {
        this.blacklisted = blacklisted;
    }

    public GoogleResult(String title, String content, int numberResult, boolean blacklisted) {
        this.title = title;
        this.content = content;
        this.numberResult = numberResult;
        this.blacklisted = blacklisted;
    }

    public GoogleResult() {
        this.title = "";
        this.content = "";
        this.numberResult = 0;
        this.blacklisted = false;
    }
}
