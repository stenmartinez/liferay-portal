/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portlet.blogs.lar;

import com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.lar.BasePortletDataHandler;
import com.liferay.portal.kernel.lar.ExportImportPathUtil;
import com.liferay.portal.kernel.lar.PortletDataContext;
import com.liferay.portal.kernel.lar.PortletDataHandlerBoolean;
import com.liferay.portal.kernel.lar.PortletDataHandlerControl;
import com.liferay.portal.kernel.lar.StagedModelDataHandlerUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.util.PortletKeys;
import com.liferay.portal.util.PropsValues;
import com.liferay.portlet.blogs.model.BlogsEntry;
import com.liferay.portlet.blogs.service.BlogsEntryLocalServiceUtil;
import com.liferay.portlet.blogs.service.BlogsStatsUserLocalServiceUtil;
import com.liferay.portlet.blogs.service.persistence.BlogsEntryActionableDynamicQuery;
import com.liferay.portlet.journal.lar.JournalPortletDataHandler;

import java.util.List;

import javax.portlet.PortletPreferences;

/**
 * @author Bruno Farache
 * @author Raymond Augé
 * @author Juan Fernández
 * @author Zsolt Berentey
 */
public class BlogsPortletDataHandler extends BasePortletDataHandler {

	public static final String NAMESPACE = "blogs";

	public BlogsPortletDataHandler() {
		setAlwaysExportable(true);
		setExportControls(
			new PortletDataHandlerBoolean(NAMESPACE, "entries", true, true));
		setExportMetadataControls(
			new PortletDataHandlerBoolean(
				NAMESPACE, "blog-entries", true,
				new PortletDataHandlerControl[] {
					new PortletDataHandlerBoolean(NAMESPACE, "categories"),
					new PortletDataHandlerBoolean(NAMESPACE, "comments"),
					new PortletDataHandlerBoolean(NAMESPACE, "ratings"),
					new PortletDataHandlerBoolean(NAMESPACE, "tags")
				}));
		setImportMetadataControls(
			getExportMetadataControls()[0],
			new PortletDataHandlerBoolean(NAMESPACE, "wordpress"));
		setPublishToLiveByDefault(PropsValues.BLOGS_PUBLISH_TO_LIVE_BY_DEFAULT);
	}

	@Override
	protected PortletPreferences doDeleteData(
			PortletDataContext portletDataContext, String portletId,
			PortletPreferences portletPreferences)
		throws Exception {

		if (portletDataContext.addPrimaryKey(
				BlogsPortletDataHandler.class, "deleteData")) {

			return portletPreferences;
		}

		BlogsEntryLocalServiceUtil.deleteEntries(
			portletDataContext.getScopeGroupId());

		BlogsStatsUserLocalServiceUtil.deleteStatsUserByGroupId(
			portletDataContext.getScopeGroupId());

		return portletPreferences;
	}

	@Override
	protected String doExportData(
			final PortletDataContext portletDataContext, String portletId,
			PortletPreferences portletPreferences)
		throws Exception {

		portletDataContext.addPermissions(
			"com.liferay.portlet.blogs", portletDataContext.getScopeGroupId());

		Element rootElement = addExportDataRootElement(portletDataContext);

		rootElement.addAttribute(
			"group-id", String.valueOf(portletDataContext.getScopeGroupId()));

		ActionableDynamicQuery entryActionableDynamicQuery =
			new BlogsEntryActionableDynamicQuery() {
				
			@Override
			protected void addCriteria(DynamicQuery dynamicQuery) {
				portletDataContext.addDateRangeCriteria(
					dynamicQuery, "modifiedDate");
			}

			protected void performAction(Object object) throws PortalException {
				BlogsEntry entry = (BlogsEntry)object;

				StagedModelDataHandlerUtil.exportStagedModel(
					portletDataContext, entry);
			}

		};

		entryActionableDynamicQuery.setGroupId(
			portletDataContext.getScopeGroupId());

		entryActionableDynamicQuery.performActions();

		return getExportDataRootElementString(rootElement);
	}

	@Override
	protected PortletPreferences doImportData(
			PortletDataContext portletDataContext, String portletId,
			PortletPreferences portletPreferences, String data)
		throws Exception {

		portletDataContext.importPermissions(
			"com.liferay.portlet.blogs", portletDataContext.getSourceGroupId(),
			portletDataContext.getScopeGroupId());

		Element entriesElement = portletDataContext.getImportDataGroupElement(
			BlogsEntry.class);

		if (entriesElement != null) {
			JournalPortletDataHandler.importReferenceData(
				portletDataContext, entriesElement);
		}

		List<Element> entryElements = entriesElement.elements();

		for (Element entryElement : entryElements) {
			StagedModelDataHandlerUtil.importStagedModel(
				portletDataContext, entryElement);
		}

		if (portletDataContext.getBooleanParameter(NAMESPACE, "wordpress")) {
			WordPressImporter.importData(portletDataContext);
		}

		return null;
	}

	protected void exportEntry(
			PortletDataContext portletDataContext, Element entriesElement,
			Element dlFileEntryTypesElement, Element dlFoldersElement,
			Element dlFileEntriesElement, Element dlFileRanksElement,
			Element dlRepositoriesElement, Element dlRepositoryEntriesElement,
			BlogsEntry entry)
		throws Exception {
	}

	protected String getEntryImagePath(
			PortletDataContext portletDataContext, BlogsEntry entry)
		throws Exception {

		StringBundler sb = new StringBundler(4);

		sb.append(
			ExportImportPathUtil.getPortletPath(
				portletDataContext, PortletKeys.BLOGS));
		sb.append("/entry/");
		sb.append(entry.getUuid());
		sb.append(StringPool.SLASH);

		return sb.toString();
	}

	protected String getEntryPath(
		PortletDataContext portletDataContext, BlogsEntry entry) {

		StringBundler sb = new StringBundler(4);

		sb.append(
			ExportImportPathUtil.getPortletPath(
				portletDataContext, PortletKeys.BLOGS));
		sb.append("/entries/");
		sb.append(entry.getEntryId());
		sb.append(".xml");

		return sb.toString();
	}

	protected String getEntrySmallImagePath(
			PortletDataContext portletDataContext, BlogsEntry entry)
		throws Exception {

		StringBundler sb = new StringBundler(6);

		sb.append(
			ExportImportPathUtil.getPortletPath(
				portletDataContext, PortletKeys.BLOGS));
		sb.append("/entries/");
		sb.append(entry.getUuid());
		sb.append("/thumbnail");
		sb.append(StringPool.PERIOD);
		sb.append(entry.getSmallImageType());

		return sb.toString();
	}

	protected void importEntry(
			PortletDataContext portletDataContext, Element entryElement,
			BlogsEntry entry)
		throws Exception {
	}

}