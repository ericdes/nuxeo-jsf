/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: LayoutRowTagHandler.java 30553 2008-02-24 15:51:31Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets;

import java.io.IOException;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.api.Layout;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRow;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletException;
import com.sun.facelets.el.VariableMapperWrapper;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagHandler;

/**
 * Layout row recursion tag handler.
 * <p>
 * Iterate over the layout rows and apply next handlers as many times as needed.
 * <p>
 * Only works when used inside a tag using the {@link LayoutTagHandler} template
 * client.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class LayoutRowTagHandler extends TagHandler {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(LayoutRowTagHandler.class);

    protected final TagConfig config;

    public LayoutRowTagHandler(TagConfig config) {
        super(config);
        this.config = config;
    }

    /**
     * For each row in layout, exposes row variables and applies next handler.
     * <p>
     * Needs layout to be exposed in context, so works in conjunction with
     * {@link LayoutTagHandler}.
     * <p>
     * Row variables exposed: {@link RenderVariables.rowVariables#row} and
     * {@link RenderVariables.rowVariables#rowIndex}.
     */
    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, FaceletException, ELException {
        // resolve rows from layout in context
        Layout layout = null;
        String layoutVariableName = RenderVariables.layoutVariables.layout.name();
        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, config);
        TagAttribute layoutAttribute = helper.createAttribute(
                layoutVariableName, String.format("#{%s}", layoutVariableName));
        if (layoutAttribute != null) {
            layout = (Layout) layoutAttribute.getObject(ctx, Layout.class);
        }
        if (layout == null) {
            log.error("Could not resolve layout " + layoutAttribute);
            return;
        }

        LayoutRow[] rows = layout.getRows();
        if (rows == null || rows.length == 0) {
            return;
        }

        VariableMapper orig = ctx.getVariableMapper();
        ctx.setVariableMapper(new VariableMapperWrapper(orig));
        try {
            int rowCounter = 0;
            for (LayoutRow row : rows) {
                // expose row variables
                VariableMapper vm = ctx.getVariableMapper();
                ValueExpression rowVe = ctx.getExpressionFactory().createValueExpression(
                        row, LayoutRow.class);
                vm.setVariable(RenderVariables.rowVariables.layoutRow.name(),
                        rowVe);
                ValueExpression rowIndexVe = ctx.getExpressionFactory().createValueExpression(
                        rowCounter, Integer.class);
                vm.setVariable(
                        RenderVariables.rowVariables.layoutRowIndex.name(),
                        rowIndexVe);
                nextHandler.apply(ctx, parent);
                rowCounter++;
            }
        } finally {
            ctx.setVariableMapper(orig);
        }
    }
}
