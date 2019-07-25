/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
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

package com.liferay.fragment.entry.processor.freemarker;

import com.liferay.fragment.entry.processor.freemarker.configuration.FreeMarkerFragmentEntryProcessorConfiguration;
import com.liferay.fragment.exception.FragmentEntryContentException;
import com.liferay.fragment.model.FragmentEntryLink;
import com.liferay.fragment.processor.FragmentEntryProcessor;
import com.liferay.fragment.processor.FragmentEntryProcessorContext;
import com.liferay.fragment.util.FragmentEntryConfigUtil;
import com.liferay.fragment.util.configuration.FragmentConfigurationField;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.io.unsync.UnsyncStringWriter;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.template.StringTemplateResource;
import com.liferay.portal.kernel.template.Template;
import com.liferay.portal.kernel.template.TemplateConstants;
import com.liferay.portal.kernel.template.TemplateException;
import com.liferay.portal.kernel.template.TemplateManager;
import com.liferay.portal.kernel.template.TemplateManagerUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.segments.constants.SegmentsConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

/**
 * @author Pavel Savinov
 */
@Component(
	configurationPid = "com.liferay.fragment.entry.processor.freemarker.configuration.FreeMarkerFragmentEntryProcessorConfiguration",
	immediate = true, property = "fragment.entry.processor.priority:Integer=1",
	service = FragmentEntryProcessor.class
)
public class FreeMarkerFragmentEntryProcessor
	implements FragmentEntryProcessor {

	@Override
	public JSONObject getDefaultEditableValuesJSONObject(
		String html, String configuration) {

		return JSONFactoryUtil.createJSONObject();
	}

	@Override
	public String processFragmentEntryLinkHTML(
			FragmentEntryLink fragmentEntryLink, String html,
			FragmentEntryProcessorContext fragmentEntryProcessorContext)
		throws PortalException {

		if (!_freeMarkerFragmentEntryProcessorConfiguration.enable()) {
			return html;
		}

		if (fragmentEntryProcessorContext.getHttpServletRequest() == null) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"HTTP servlet request is not set in the fragment entry " +
						"processor context");
			}

			return html;
		}

		if (fragmentEntryProcessorContext.getHttpServletResponse() == null) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"HTTP servlet response is not set in the fragment entry " +
						"processor context");
			}

			return html;
		}

		UnsyncStringWriter unsyncStringWriter = new UnsyncStringWriter();

		Template template = TemplateManagerUtil.getTemplate(
			TemplateConstants.LANG_TYPE_FTL,
			new StringTemplateResource("template_id", "[#ftl]\n" + html), true);

		template.put(TemplateConstants.WRITER, unsyncStringWriter);

		TemplateManager templateManager =
			TemplateManagerUtil.getTemplateManager(
				TemplateConstants.LANG_TYPE_FTL);

		Map<String, Object> contextObjects = new HashMap<>();

		JSONObject configurationValuesJSONObject = _getConfigurationJSONObject(
			fragmentEntryLink.getConfiguration(),
			fragmentEntryLink.getEditableValues(),
			fragmentEntryProcessorContext.getSegmentsExperienceIds());

		contextObjects.put("configuration", configurationValuesJSONObject);

		contextObjects.putAll(
			FragmentEntryConfigUtil.getContextObjects(
				configurationValuesJSONObject,
				fragmentEntryLink.getConfiguration()));

		templateManager.addContextObjects(template, contextObjects);

		templateManager.addTaglibSupport(
			template, fragmentEntryProcessorContext.getHttpServletRequest(),
			fragmentEntryProcessorContext.getHttpServletResponse());

		template.prepare(fragmentEntryProcessorContext.getHttpServletRequest());

		try {
			template.processTemplate(unsyncStringWriter);
		}
		catch (TemplateException te) {
			throw new FragmentEntryContentException(_getMessage(te), te);
		}

		return unsyncStringWriter.toString();
	}

	@Override
	public void validateFragmentEntryHTML(String html, String configuration)
		throws PortalException {

		if (!_freeMarkerFragmentEntryProcessorConfiguration.enable()) {
			return;
		}

		Template template = TemplateManagerUtil.getTemplate(
			TemplateConstants.LANG_TYPE_FTL,
			new StringTemplateResource("template_id", "[#ftl]\n" + html), true);

		try {
			HttpServletRequest httpServletRequest = null;
			HttpServletResponse httpServletResponse = null;

			ServiceContext serviceContext =
				ServiceContextThreadLocal.getServiceContext();

			if (serviceContext != null) {
				httpServletRequest = serviceContext.getRequest();
				httpServletResponse = serviceContext.getResponse();
			}

			if ((httpServletRequest != null) && (httpServletResponse != null)) {
				TemplateManager templateManager =
					TemplateManagerUtil.getTemplateManager(
						TemplateConstants.LANG_TYPE_FTL);

				Map<String, Object> contextObjects = new HashMap<>();

				JSONObject configurationDefaultValuesJSONObject =
					FragmentEntryConfigUtil.
						getConfigurationDefaultValuesJSONObject(configuration);

				contextObjects.put(
					"configuration", configurationDefaultValuesJSONObject);

				contextObjects.putAll(
					FragmentEntryConfigUtil.getContextObjects(
						configurationDefaultValuesJSONObject, configuration));

				templateManager.addContextObjects(template, contextObjects);

				templateManager.addTaglibSupport(
					template, httpServletRequest, httpServletResponse);

				template.prepare(httpServletRequest);

				template.processTemplate(new UnsyncStringWriter());
			}
		}
		catch (TemplateException te) {
			throw new FragmentEntryContentException(_getMessage(te), te);
		}
	}

	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {
		_freeMarkerFragmentEntryProcessorConfiguration =
			ConfigurableUtil.createConfigurable(
				FreeMarkerFragmentEntryProcessorConfiguration.class,
				properties);
	}

	private JSONObject _getConfigurationJSONObject(
			String configuration, String editableValues,
			long[] segmentsExperienceIds)
		throws JSONException {

		JSONObject configurationDefaultValuesJSONObject =
			FragmentEntryConfigUtil.getConfigurationDefaultValuesJSONObject(
				configuration);

		if (configurationDefaultValuesJSONObject == null) {
			return JSONFactoryUtil.createJSONObject();
		}

		JSONObject editableValuesJSONObject = JSONFactoryUtil.createJSONObject(
			editableValues);

		Class<?> clazz = getClass();

		String className = clazz.getName();

		JSONObject configurationValuesJSONObject =
			editableValuesJSONObject.getJSONObject(className);

		if (configurationValuesJSONObject == null) {
			return configurationDefaultValuesJSONObject;
		}

		JSONObject configurationJSONObject = _getSegmentedConfigurationValues(
			segmentsExperienceIds, configurationValuesJSONObject);

		List<FragmentConfigurationField> configurationFields =
			FragmentEntryConfigUtil.getFragmentConfigurationFields(
				configuration);

		for (FragmentConfigurationField configurationField :
				configurationFields) {

			String name = configurationField.getName();

			Object object = configurationJSONObject.get(name);

			if (Validator.isNull(object)) {
				continue;
			}

			configurationDefaultValuesJSONObject.put(
				name,
				FragmentEntryConfigUtil.getFieldValue(
					configurationField,
					configurationJSONObject.getString(name)));
		}

		return configurationDefaultValuesJSONObject;
	}

	private String _getMessage(TemplateException te) {
		ResourceBundle resourceBundle = ResourceBundleUtil.getBundle(
			"content.Language", getClass());

		String message = LanguageUtil.get(
			resourceBundle, "freemarker-syntax-is-invalid");

		Throwable cause = te.getCause();

		String causeMessage = cause.getLocalizedMessage();

		if (Validator.isNotNull(causeMessage)) {
			message = message + "\n\n" + causeMessage;
		}

		return message;
	}

	private JSONObject _getSegmentedConfigurationValues(
		long[] segmentsExperienceIds,
		JSONObject configurationValuesJSONObject) {

		long segmentsExperienceId =
			SegmentsConstants.SEGMENTS_EXPERIENCE_ID_DEFAULT;

		if (segmentsExperienceIds.length > 0) {
			segmentsExperienceId = segmentsExperienceIds[0];
		}

		JSONObject configurationJSONObject =
			configurationValuesJSONObject.getJSONObject(
				SegmentsConstants.SEGMENTS_EXPERIENCE_ID_PREFIX +
					segmentsExperienceId);

		if (configurationJSONObject == null) {
			configurationJSONObject = JSONFactoryUtil.createJSONObject();
		}

		return configurationJSONObject;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		FreeMarkerFragmentEntryProcessor.class);

	private volatile FreeMarkerFragmentEntryProcessorConfiguration
		_freeMarkerFragmentEntryProcessorConfiguration;

}