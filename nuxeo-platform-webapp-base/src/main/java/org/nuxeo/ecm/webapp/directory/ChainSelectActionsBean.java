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
 *     Nuxeo - initial API and implementation
 *
 * $Id: ChainSelectActionsBean.java 28950 2008-01-11 13:35:06Z tdelprat $
 */

package org.nuxeo.ecm.webapp.directory;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remove;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.nuxeo.ecm.platform.ui.web.directory.ChainSelect;
import org.nuxeo.ecm.platform.ui.web.directory.ChainSelectStatus;
import org.nuxeo.ecm.platform.ui.web.directory.Selection;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.webapp.base.InputController;

/**
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
@Name("chainSelectActions")
@Scope(SESSION)
public class ChainSelectActionsBean extends InputController implements
        ChainSelectActions, Serializable {

    private static final long serialVersionUID = 27502317512904295L;
    private static final Log log = LogFactory.getLog(ChainSelectActionsBean.class);

    @RequestParameter("selectionValue")
    private String selectionValue;

    @PrePassivate
    public void saveState() {
        log.info("PrePassivate");
    }

    @PostActivate
    public void readState() {
        log.info("PostActivate");
    }

    @Remove
    public void destroy() {
        log.debug("Removing SEAM action listener...");
    }

    private ChainSelect getChainSelect(ActionEvent event) {
        UIComponent component = event.getComponent();
        while (!(component instanceof ChainSelect)) {
            component = component.getParent();
        }
        return (ChainSelect) component;
    }

    public void add(ActionEvent event) {
        ChainSelect chainSelect = getChainSelect(event);
        FacesContext context = FacesContext.getCurrentInstance();
        boolean allowBranchSelection = chainSelect.getBooleanProperty(
                "allowBranchSelection", false);
        boolean allowRootSelection = chainSelect.getBooleanProperty(
                "allowRootSelection", false);
        int size = chainSelect.getSize();
        String clientId = chainSelect.getClientId(context);

        LinkedHashMap<String, Selection> map = new LinkedHashMap<String, Selection>();
        for (Selection selection : chainSelect.getComponentValue()) {
            map.put(selection.getValue(chainSelect.getKeySeparator()), selection);
        }
        for (Selection selection : chainSelect.getSelections()) {
            int selectionSize = selection.getSize();
            if (!allowRootSelection && selectionSize == 0) {
                String messageStr = ComponentUtils.translate(context,
                        "label.chainSelect.empty_selection");
                FacesMessage message = new FacesMessage(messageStr);
                context.addMessage(clientId, message);
                chainSelect.setValid(false);
                return;
            }
            if (!allowBranchSelection && selectionSize > 0 && selectionSize != size) {
                String messageStr = ComponentUtils.translate(context,
                        "label.chainSelect.incomplete_selection");
                FacesMessage message = new FacesMessage(messageStr);
                context.addMessage(clientId, message);
                chainSelect.setValid(false);
                return;
            }

            map.put(selection.getValue(chainSelect.getKeySeparator()), selection);
        }

        Selection[] componentValue = map.values().toArray(new Selection[0]);

        String[] submittedValue;
        if (componentValue.length == 0) {
            submittedValue = null;
        } else {
            submittedValue = new String[componentValue.length];
            for (int i = 0; i < componentValue.length; i++) {
                submittedValue[i] = componentValue[i].getValue(chainSelect.getKeySeparator());
            }
        }

        chainSelect.setComponentValue(componentValue);
        chainSelect.setSubmittedValue(submittedValue);
        context.renderResponse();
        log.debug("add: submittedValue=" + ChainSelect.format(submittedValue));
    }

    public void delete(ActionEvent event) {
        FacesContext context = FacesContext.getCurrentInstance();
        ChainSelect chainSelect = getChainSelect(event);
        List<Selection> componentValueList = new ArrayList<Selection>();
        componentValueList.addAll(Arrays.asList(chainSelect.getComponentValue()));

        String value = context.getExternalContext().getRequestParameterMap().get(ChainSelectStatus.REMOVE_ID);

        for (Iterator<Selection> i = componentValueList.iterator(); i.hasNext();) {
            Selection selection = i.next();
            if (selection.getValue(chainSelect.getKeySeparator()).equals(value)) {
                i.remove();
            }
        }
        Selection[] componentValue = componentValueList.toArray(new Selection[0]);
        String[] submittedValue = null;
        if (componentValue.length != 0) {
            submittedValue = new String[componentValue.length];
            for (int i = 0; i < componentValue.length; i++) {
                submittedValue[i] = componentValue[i].getValue(chainSelect.getKeySeparator());
            }
        }

        chainSelect.setComponentValue(componentValue);
        chainSelect.setSubmittedValue(submittedValue);
        context.renderResponse();
    }
}
