/*
    Copyright 2013 Red Hat, Inc. and/or its affiliates.

    This file is part of jcliff.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.redhat.jcliff;

public class MatchRule {
    public final Action action;
    public final PathExpression expr;
    public final String name;
    public final int precedence;

    public MatchRule(Action action,String name,int precedence,PathExpression expr) {
        this.action=action;
        this.name=name;
        this.expr=expr;
        this.precedence=precedence;
    }

    public MatchRule(Action action,String name,int precedence,String expr) {
        this.action=action;
        this.name=name;
        this.precedence=precedence;
        this.expr=PathExpression.parse(expr);
    }
}
