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

package com.liferay.knowledge.base.service.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.asset.kernel.service.AssetEntryLocalServiceUtil;
import com.liferay.knowledge.base.constants.KBArticleConstants;
import com.liferay.knowledge.base.constants.KBFolderConstants;
import com.liferay.knowledge.base.exception.DuplicateKBArticleExternalReferenceCodeException;
import com.liferay.knowledge.base.exception.KBArticleContentException;
import com.liferay.knowledge.base.exception.KBArticleParentException;
import com.liferay.knowledge.base.exception.KBArticleSourceURLException;
import com.liferay.knowledge.base.exception.KBArticleStatusException;
import com.liferay.knowledge.base.exception.KBArticleTitleException;
import com.liferay.knowledge.base.exception.KBArticleUrlTitleException;
import com.liferay.knowledge.base.model.KBArticle;
import com.liferay.knowledge.base.model.KBFolder;
import com.liferay.knowledge.base.service.KBArticleLocalServiceUtil;
import com.liferay.knowledge.base.service.KBCommentLocalServiceUtil;
import com.liferay.knowledge.base.service.KBFolderLocalServiceUtil;
import com.liferay.knowledge.base.util.comparator.KBArticlePriorityComparator;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.ModelHintsUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.service.ClassNameLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.WorkflowDefinitionLinkLocalServiceUtil;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.ratings.kernel.service.RatingsEntryLocalServiceUtil;
import com.liferay.ratings.kernel.service.RatingsStatsLocalServiceUtil;
import com.liferay.subscription.service.SubscriptionLocalServiceUtil;

import java.io.InputStream;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Adolfo Pérez
 * @author Roberto Díaz
 */
@RunWith(Arquillian.class)
public class KBArticleLocalServiceTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		_group = GroupTestUtil.addGroup();
		_kbArticleClassNameId = ClassNameLocalServiceUtil.getClassNameId(
			KBArticleConstants.getClassName());
		_kbFolderClassNameId = ClassNameLocalServiceUtil.getClassNameId(
			KBFolderConstants.getClassName());

		_user = TestPropsValues.getUser();

		_serviceContext = ServiceContextTestUtil.getServiceContext(
			_group, _user.getUserId());
	}

	@Test
	public void testAddApprovedKBArticleInsideApprovedKBArticle()
		throws Exception {

		_serviceContext.setWorkflowAction(WorkflowConstants.ACTION_PUBLISH);

		KBArticle parentKBArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			null, _serviceContext);

		KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), parentKBArticle.getClassNameId(),
			parentKBArticle.getResourcePrimKey(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);
	}

	@Test(expected = KBArticleStatusException.class)
	public void testAddApprovedKBArticleInsideDraftKBArticle()
		throws Exception {

		_serviceContext.setWorkflowAction(WorkflowConstants.ACTION_SAVE_DRAFT);

		KBArticle parentKBArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			null, _serviceContext);

		_serviceContext.setWorkflowAction(WorkflowConstants.ACTION_PUBLISH);

		KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), parentKBArticle.getClassNameId(),
			parentKBArticle.getResourcePrimKey(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);
	}

	@Test
	public void testAddApprovedKBArticleInsideNonlatestApprovedKBArticle()
		throws Exception {

		_serviceContext.setWorkflowAction(WorkflowConstants.ACTION_PUBLISH);

		KBArticle parentKBArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			null, _serviceContext);

		_serviceContext.setWorkflowAction(WorkflowConstants.ACTION_SAVE_DRAFT);

		parentKBArticle = KBArticleLocalServiceUtil.updateKBArticle(
			_user.getUserId(), parentKBArticle.getResourcePrimKey(),
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, null, _serviceContext);

		_serviceContext.setWorkflowAction(WorkflowConstants.ACTION_PUBLISH);

		KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), parentKBArticle.getClassNameId(),
			parentKBArticle.getResourcePrimKey(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);
	}

	@Test
	public void testAddDraftKBArticleInsideApprovedKBArticle()
		throws Exception {

		_serviceContext.setWorkflowAction(WorkflowConstants.ACTION_PUBLISH);

		KBArticle parentKBArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			null, _serviceContext);

		_serviceContext.setWorkflowAction(WorkflowConstants.ACTION_SAVE_DRAFT);

		KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), parentKBArticle.getClassNameId(),
			parentKBArticle.getResourcePrimKey(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);
	}

	@Test
	public void testAddDraftKBArticleInsideDraftKBArticle() throws Exception {
		_serviceContext.setWorkflowAction(WorkflowConstants.ACTION_SAVE_DRAFT);

		KBArticle parentKBArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			null, _serviceContext);

		KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), parentKBArticle.getClassNameId(),
			parentKBArticle.getResourcePrimKey(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);
	}

	@Test
	public void testAddKBArticlesMarkdownWithNoWorkflow() throws Exception {
		updateWorkflowDefinitionForKBArticle("");

		importMarkdownArticles();
	}

	@Test
	public void testAddKBArticlesMarkdownWithSingleApproverWorkflow()
		throws Exception {

		updateWorkflowDefinitionForKBArticle("Single Approver@1");

		importMarkdownArticles();
	}

	@Test(expected = KBArticleContentException.class)
	public void testAddKBArticleWithBlankContent() throws Exception {
		String content = StringPool.BLANK;

		KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(), content,
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			_serviceContext);
	}

	@Test
	public void testAddKBArticleWithBlankSourceURL() throws Exception {
		String sourceURL = StringPool.BLANK;

		KBArticle kbArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), sourceURL,
			null, null, _serviceContext);

		Assert.assertTrue(Validator.isNull(kbArticle.getSourceURL()));
	}

	@Test(expected = KBArticleTitleException.class)
	public void testAddKBArticleWithBlankTitle() throws Exception {
		String title = StringPool.BLANK;

		KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID, title,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			_serviceContext);
	}

	@Test
	public void testAddKBArticleWithBlankURLTitle() throws Exception {
		String urlTitle = StringPool.BLANK;

		KBArticle kbArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), urlTitle, StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);

		Assert.assertTrue(Validator.isNotNull(kbArticle.getUrlTitle()));
	}

	@Test
	public void testAddKBArticleWithCustomHTML() throws Exception {
		String name = PrincipalThreadLocal.getName();

		try {
			PrincipalThreadLocal.setName(TestPropsValues.getUserId());

			String content =
				"<a href=\"http://www.liferay.com\" target=\"_blank\" />";

			KBArticle kbArticle = KBArticleLocalServiceUtil.addKBArticle(
				null, _user.getUserId(), _kbFolderClassNameId,
				KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
				StringUtil.randomString(), StringUtil.randomString(), content,
				StringUtil.randomString(), null, null, null, _serviceContext);

			Matcher matcher = _tagetBlankPattern.matcher(
				kbArticle.getContent());

			Assert.assertTrue(matcher.matches());
		}
		finally {
			PrincipalThreadLocal.setName(name);
		}
	}

	@Test(expected = KBArticleUrlTitleException.class)
	public void testAddKBArticleWithDuplicateURLTitle() throws Exception {
		String urlTitle = StringUtil.randomString();

		KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), urlTitle, StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);

		KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), urlTitle, StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);
	}

	@Test(expected = DuplicateKBArticleExternalReferenceCodeException.class)
	public void testAddKBArticleWithExistingExternalReferenceCode()
		throws Exception {

		KBArticle kbArticle = KBArticleLocalServiceUtil.addKBArticle(
			RandomTestUtil.randomString(), _user.getUserId(),
			_kbFolderClassNameId, KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(),
			StringPool.BLANK, null, null, _serviceContext);

		KBArticleLocalServiceUtil.addKBArticle(
			kbArticle.getExternalReferenceCode(), _user.getUserId(),
			_kbFolderClassNameId, KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(),
			StringPool.BLANK, null, null, _serviceContext);
	}

	@Test
	public void testAddKBArticleWithExternalReferenceCode() throws Exception {
		String externalReferenceCode = RandomTestUtil.randomString();

		KBArticle kbArticle = KBArticleLocalServiceUtil.addKBArticle(
			externalReferenceCode, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(),
			StringPool.BLANK, null, null, _serviceContext);

		Assert.assertEquals(
			externalReferenceCode, kbArticle.getExternalReferenceCode());
	}

	@Test(expected = KBArticleParentException.class)
	public void testAddKBArticleWithInvalidParentClassNameId()
		throws Exception {

		long invalidParentClassNameId = 123456789L;

		KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), invalidParentClassNameId,
			RandomTestUtil.nextLong(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);
	}

	@Test(expected = KBArticleSourceURLException.class)
	public void testAddKBArticleWithInvalidSourceURL() throws Exception {
		String sourceURL = "InvalidURL";

		KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), sourceURL,
			null, null, _serviceContext);
	}

	@Test(expected = KBArticleUrlTitleException.class)
	public void testAddKBArticleWithInvalidURLTitle() throws Exception {
		String invalidURLTitle = "#$%&/(";

		KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), invalidURLTitle,
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			null, _serviceContext);
	}

	@Test(expected = KBArticleUrlTitleException.class)
	public void testAddKBArticleWithLargeURLTitle() throws Exception {
		int urlTitleMaxSize = ModelHintsUtil.getMaxLength(
			KBArticle.class.getName(), "urlTitle");

		String invalidURLTitle = StringUtil.randomString(urlTitleMaxSize + 1);

		KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), invalidURLTitle,
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			null, _serviceContext);
	}

	@Test(expected = KBArticleContentException.class)
	public void testAddKBArticleWithNullContent() throws Exception {
		String content = null;

		KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(), content,
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			_serviceContext);
	}

	@Test
	public void testAddKBArticleWithNullSourceURL() throws Exception {
		String sourceURL = null;

		KBArticle kbArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), sourceURL,
			null, null, _serviceContext);

		Assert.assertTrue(Validator.isNull(kbArticle.getSourceURL()));
	}

	@Test(expected = KBArticleTitleException.class)
	public void testAddKBArticleWithNullTitle() throws Exception {
		String title = null;

		KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID, title,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			_serviceContext);
	}

	@Test
	public void testAddKBArticleWithNullURLTitle() throws Exception {
		String urlTitle = null;

		KBArticle kbArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), urlTitle, StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);

		Assert.assertTrue(Validator.isNotNull(kbArticle.getUrlTitle()));
	}

	@Test
	public void testAddKBArticleWithValidParentKBArticle() throws Exception {
		KBArticle kbArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			null, _serviceContext);

		KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbArticleClassNameId,
			kbArticle.getResourcePrimKey(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);
	}

	@Test
	public void testAddKBArticleWithValidParentKBFolder() throws Exception {
		KBFolder kbFolder = KBFolderLocalServiceUtil.addKBFolder(
			null, _user.getUserId(), _group.getGroupId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			_serviceContext);

		KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			kbFolder.getPrimaryKey(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);
	}

	@Test
	public void testAddKBArticleWithValidSourceURL() throws Exception {
		String sourceURL = "http://www.liferay.com";

		KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), sourceURL,
			null, null, _serviceContext);
	}

	@Test
	public void testBuildTreePathAfterMoveKBArticle() throws Exception {
		KBFolder kbFolder = KBFolderLocalServiceUtil.addKBFolder(
			null, _user.getUserId(), _group.getGroupId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			_serviceContext);

		KBFolder childKBFolder = KBFolderLocalServiceUtil.addKBFolder(
			null, _user.getUserId(), _group.getGroupId(), _kbFolderClassNameId,
			kbFolder.getKbFolderId(), StringUtil.randomString(),
			StringUtil.randomString(), _serviceContext);

		KBArticle kbArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			childKBFolder.getKbFolderId(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringPool.BLANK, null, null,
			_serviceContext);

		String originalKBArticleTreePath = kbArticle.buildTreePath();

		KBArticleLocalServiceUtil.moveKBArticle(
			_user.getUserId(), kbArticle.getResourcePrimKey(),
			_kbFolderClassNameId, KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			kbArticle.getPriority());

		kbArticle = KBArticleLocalServiceUtil.getLatestKBArticle(
			kbArticle.getResourcePrimKey(), WorkflowConstants.STATUS_ANY);

		String newKBArticleTreePath = String.valueOf(CharPool.SLASH);

		Assert.assertEquals(newKBArticleTreePath, kbArticle.buildTreePath());

		KBArticleLocalServiceUtil.moveKBArticle(
			_user.getUserId(), kbArticle.getResourcePrimKey(),
			_kbFolderClassNameId, childKBFolder.getKbFolderId(),
			kbArticle.getPriority());

		kbArticle = KBArticleLocalServiceUtil.getLatestKBArticle(
			kbArticle.getResourcePrimKey(), WorkflowConstants.STATUS_ANY);

		Assert.assertEquals(
			originalKBArticleTreePath, kbArticle.buildTreePath());
	}

	@Test
	public void testBuildTreePathAfterMoveKBFolder() throws Exception {
		KBFolder kbFolder = KBFolderLocalServiceUtil.addKBFolder(
			null, _user.getUserId(), _group.getGroupId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			_serviceContext);

		KBFolder childKBFolder = KBFolderLocalServiceUtil.addKBFolder(
			null, _user.getUserId(), _group.getGroupId(), _kbFolderClassNameId,
			kbFolder.getKbFolderId(), StringUtil.randomString(),
			StringUtil.randomString(), _serviceContext);

		KBArticle kbArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			childKBFolder.getKbFolderId(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringPool.BLANK, null, null,
			_serviceContext);

		String originalKBArticleTreePath = kbArticle.buildTreePath();

		KBFolderLocalServiceUtil.moveKBFolder(
			childKBFolder.getKbFolderId(),
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID);

		String newKBArticleTreePath = StringBundler.concat(
			CharPool.SLASH, childKBFolder.getKbFolderId(), CharPool.SLASH);

		Assert.assertEquals(newKBArticleTreePath, kbArticle.buildTreePath());

		KBFolderLocalServiceUtil.moveKBFolder(
			childKBFolder.getKbFolderId(), kbFolder.getKbFolderId());
		Assert.assertEquals(
			originalKBArticleTreePath, kbArticle.buildTreePath());
	}

	@Test
	public void testDeleteGroupKBArticlesDeletesKBArticles() throws Exception {
		KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			null, _serviceContext);

		Assert.assertEquals(
			1,
			KBArticleLocalServiceUtil.getGroupKBArticlesCount(
				_group.getGroupId(), WorkflowConstants.STATUS_ANY));

		KBArticleLocalServiceUtil.deleteGroupKBArticles(_group.getGroupId());

		Assert.assertEquals(
			0,
			KBArticleLocalServiceUtil.getGroupKBArticlesCount(
				_group.getGroupId(), WorkflowConstants.STATUS_ANY));
	}

	@Test
	public void testDeleteGroupKBArticlesDeletesSubscriptions()
		throws Exception {

		KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			null, _serviceContext);

		int subscriptionsCount =
			SubscriptionLocalServiceUtil.getUserSubscriptionsCount(
				_user.getUserId());

		KBArticleLocalServiceUtil.subscribeGroupKBArticles(
			_user.getUserId(), _group.getGroupId());

		Assert.assertEquals(
			subscriptionsCount + 1,
			SubscriptionLocalServiceUtil.getUserSubscriptionsCount(
				_user.getUserId()));

		KBArticleLocalServiceUtil.deleteGroupKBArticles(_group.getGroupId());

		Assert.assertEquals(
			subscriptionsCount,
			SubscriptionLocalServiceUtil.getUserSubscriptionsCount(
				_user.getUserId()));
	}

	@Test
	public void testDeleteKBArticleDeletesAssetEntry() throws Exception {
		KBArticle kbArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			null, _serviceContext);

		Assert.assertNotNull(
			AssetEntryLocalServiceUtil.fetchEntry(
				KBArticleConstants.getClassName(), kbArticle.getClassPK()));

		KBArticleLocalServiceUtil.deleteKBArticle(kbArticle);

		Assert.assertNull(
			AssetEntryLocalServiceUtil.fetchEntry(
				KBArticleConstants.getClassName(), kbArticle.getClassPK()));
	}

	@Test
	public void testDeleteKBArticleDeletesChildKBArticles() throws Exception {
		KBArticle kbArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			null, _serviceContext);

		KBArticle childKBArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), kbArticle.getClassNameId(),
			kbArticle.getResourcePrimKey(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);

		KBArticleLocalServiceUtil.deleteKBArticle(kbArticle);

		Assert.assertNull(
			KBArticleLocalServiceUtil.fetchKBArticle(
				childKBArticle.getKbArticleId()));
	}

	@Test
	public void testDeleteKBArticleDeletesKBComments() throws Exception {
		KBArticle kbArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			null, _serviceContext);

		KBCommentLocalServiceUtil.addKBComment(
			_user.getUserId(), kbArticle.getClassNameId(),
			kbArticle.getClassPK(), StringUtil.randomString(), _serviceContext);

		KBArticleLocalServiceUtil.deleteKBArticle(kbArticle);

		Assert.assertEquals(
			0,
			KBCommentLocalServiceUtil.getKBCommentsCount(
				KBArticleConstants.getClassName(), kbArticle.getClassPK()));
	}

	@Test
	public void testDeleteKBArticleDeletesRatings() throws Exception {
		KBArticle kbArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			null, _serviceContext);

		RatingsEntryLocalServiceUtil.updateEntry(
			_user.getUserId(), KBArticleConstants.getClassName(),
			kbArticle.getClassPK(), 1.0, _serviceContext);

		KBArticleLocalServiceUtil.deleteKBArticle(kbArticle);

		Assert.assertNull(
			RatingsStatsLocalServiceUtil.fetchStats(
				KBArticleConstants.getClassName(), kbArticle.getClassPK()));
	}

	@Test
	public void testGetAllDescendantKBArticles() throws Exception {
		KBArticle parentKBArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID, "Parent Article",
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);

		KBArticle childKBArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbArticleClassNameId,
			parentKBArticle.getResourcePrimKey(), "Child Article",
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);

		KBArticle grandchildKBArticleA = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbArticleClassNameId,
			childKBArticle.getResourcePrimKey(), "Grandchild Article A",
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);

		KBArticle greatGrandchildKBArticleA =
			KBArticleLocalServiceUtil.addKBArticle(
				null, _user.getUserId(), _kbArticleClassNameId,
				grandchildKBArticleA.getResourcePrimKey(),
				"GreatGrandchild Article A", StringUtil.randomString(),
				StringUtil.randomString(), StringUtil.randomString(), null,
				null, null, _serviceContext);

		KBArticle grandchildKBArticleB = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbArticleClassNameId,
			childKBArticle.getResourcePrimKey(), "Grandchild Article B",
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);

		KBArticle greatGrandchildKBArticleB =
			KBArticleLocalServiceUtil.addKBArticle(
				null, _user.getUserId(), _kbArticleClassNameId,
				grandchildKBArticleB.getResourcePrimKey(),
				"GreatGrandchild Article B", StringUtil.randomString(),
				StringUtil.randomString(), StringUtil.randomString(), null,
				null, null, _serviceContext);

		List<KBArticle> kbArticleAndAllDescendantKBArticles =
			KBArticleLocalServiceUtil.getAllDescendantKBArticles(
				parentKBArticle.getResourcePrimKey(),
				WorkflowConstants.STATUS_APPROVED,
				new KBArticlePriorityComparator(true));

		Assert.assertEquals(
			kbArticleAndAllDescendantKBArticles.toString(), 5,
			kbArticleAndAllDescendantKBArticles.size());

		KBArticle currentChildKBArticle =
			kbArticleAndAllDescendantKBArticles.get(0);
		KBArticle currentGrandchildKBArticleA =
			kbArticleAndAllDescendantKBArticles.get(1);
		KBArticle currentGreatGrandchildKBArticleA =
			kbArticleAndAllDescendantKBArticles.get(2);
		KBArticle currentGrandchildKBArticleB =
			kbArticleAndAllDescendantKBArticles.get(3);
		KBArticle currentGreatGrandchildKBArticleB =
			kbArticleAndAllDescendantKBArticles.get(4);

		Assert.assertEquals(
			childKBArticle.getResourcePrimKey(),
			currentChildKBArticle.getResourcePrimKey());
		Assert.assertEquals(
			grandchildKBArticleA.getResourcePrimKey(),
			currentGrandchildKBArticleA.getResourcePrimKey());
		Assert.assertEquals(
			greatGrandchildKBArticleA.getResourcePrimKey(),
			currentGreatGrandchildKBArticleA.getResourcePrimKey());
		Assert.assertEquals(
			grandchildKBArticleB.getResourcePrimKey(),
			currentGrandchildKBArticleB.getResourcePrimKey());
		Assert.assertEquals(
			greatGrandchildKBArticleB.getResourcePrimKey(),
			currentGreatGrandchildKBArticleB.getResourcePrimKey());
	}

	@Test
	public void testGetKBArticleAndAllDescendantKBArticles() throws Exception {
		KBArticle parentKBArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID, "Parent Article",
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);

		KBArticle childKBArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbArticleClassNameId,
			parentKBArticle.getResourcePrimKey(), "Child Article",
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);

		KBArticle grandchildKBArticleA = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbArticleClassNameId,
			childKBArticle.getResourcePrimKey(), "Grandchild Article A",
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);

		KBArticle greatGrandchildKBArticleA =
			KBArticleLocalServiceUtil.addKBArticle(
				null, _user.getUserId(), _kbArticleClassNameId,
				grandchildKBArticleA.getResourcePrimKey(),
				"GreatGrandchild Article A", StringUtil.randomString(),
				StringUtil.randomString(), StringUtil.randomString(), null,
				null, null, _serviceContext);

		KBArticle grandchildKBArticleB = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbArticleClassNameId,
			childKBArticle.getResourcePrimKey(), "Grandchild Article B",
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);

		KBArticle greatGrandchildKBArticleB =
			KBArticleLocalServiceUtil.addKBArticle(
				null, _user.getUserId(), _kbArticleClassNameId,
				grandchildKBArticleB.getResourcePrimKey(),
				"GreatGrandchild Article B", StringUtil.randomString(),
				StringUtil.randomString(), StringUtil.randomString(), null,
				null, null, _serviceContext);

		List<KBArticle> kbArticleAndAllDescendantKBArticles =
			KBArticleLocalServiceUtil.getKBArticleAndAllDescendantKBArticles(
				parentKBArticle.getResourcePrimKey(),
				WorkflowConstants.STATUS_APPROVED,
				new KBArticlePriorityComparator(true));

		Assert.assertEquals(
			kbArticleAndAllDescendantKBArticles.toString(), 6,
			kbArticleAndAllDescendantKBArticles.size());

		KBArticle currentParentKBArticle =
			kbArticleAndAllDescendantKBArticles.get(0);
		KBArticle currentChildKBArticle =
			kbArticleAndAllDescendantKBArticles.get(1);
		KBArticle currentGrandchildKBArticleA =
			kbArticleAndAllDescendantKBArticles.get(2);
		KBArticle currentGreatGrandchildKBArticleA =
			kbArticleAndAllDescendantKBArticles.get(3);
		KBArticle currentGrandchildKBArticleB =
			kbArticleAndAllDescendantKBArticles.get(4);
		KBArticle currentGreatGrandchildKBArticleB =
			kbArticleAndAllDescendantKBArticles.get(5);

		Assert.assertEquals(
			parentKBArticle.getResourcePrimKey(),
			currentParentKBArticle.getResourcePrimKey());
		Assert.assertEquals(
			childKBArticle.getResourcePrimKey(),
			currentChildKBArticle.getResourcePrimKey());
		Assert.assertEquals(
			grandchildKBArticleA.getResourcePrimKey(),
			currentGrandchildKBArticleA.getResourcePrimKey());
		Assert.assertEquals(
			greatGrandchildKBArticleA.getResourcePrimKey(),
			currentGreatGrandchildKBArticleA.getResourcePrimKey());
		Assert.assertEquals(
			grandchildKBArticleB.getResourcePrimKey(),
			currentGrandchildKBArticleB.getResourcePrimKey());
		Assert.assertEquals(
			greatGrandchildKBArticleB.getResourcePrimKey(),
			currentGreatGrandchildKBArticleB.getResourcePrimKey());
	}

	@Test(expected = KBArticleParentException.class)
	public void testMoveKBArticleToInvalidParentKBArticle() throws Exception {
		KBArticle parentKBArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			null, _serviceContext);

		KBArticle childKBArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbArticleClassNameId,
			parentKBArticle.getResourcePrimKey(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);

		KBArticle grandChildKBArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbArticleClassNameId,
			childKBArticle.getResourcePrimKey(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);

		KBArticleLocalServiceUtil.moveKBArticle(
			_user.getUserId(), parentKBArticle.getResourcePrimKey(),
			_kbArticleClassNameId, grandChildKBArticle.getResourcePrimKey(),
			grandChildKBArticle.getPriority());
	}

	@Test
	public void testMoveKBArticleToParentKBArticleInHomeFolder()
		throws Exception {

		KBArticle kbArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			null, _serviceContext);

		KBArticle parentKBArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			null, _serviceContext);

		KBArticleLocalServiceUtil.moveKBArticle(
			_user.getUserId(), kbArticle.getResourcePrimKey(),
			_kbArticleClassNameId, parentKBArticle.getResourcePrimKey(),
			parentKBArticle.getPriority());

		kbArticle = KBArticleLocalServiceUtil.getLatestKBArticle(
			kbArticle.getResourcePrimKey(), WorkflowConstants.STATUS_ANY);

		Assert.assertEquals(
			_kbArticleClassNameId, kbArticle.getParentResourceClassNameId());
		Assert.assertEquals(
			parentKBArticle.getResourcePrimKey(),
			kbArticle.getParentResourcePrimKey());
	}

	@Test
	public void testMoveKBArticleToParentKBArticleInKBFolder()
		throws Exception {

		KBArticle kbArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			null, _serviceContext);

		KBFolder kbFolder = KBFolderLocalServiceUtil.addKBFolder(
			null, _user.getUserId(), _group.getGroupId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			_serviceContext);

		KBArticle parentKBArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			kbFolder.getKbFolderId(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);

		KBArticleLocalServiceUtil.moveKBArticle(
			_user.getUserId(), kbArticle.getResourcePrimKey(),
			_kbArticleClassNameId, parentKBArticle.getResourcePrimKey(),
			parentKBArticle.getPriority());

		kbArticle = KBArticleLocalServiceUtil.getLatestKBArticle(
			kbArticle.getResourcePrimKey(), WorkflowConstants.STATUS_ANY);

		Assert.assertEquals(
			_kbArticleClassNameId, kbArticle.getParentResourceClassNameId());
		Assert.assertEquals(
			parentKBArticle.getResourcePrimKey(),
			kbArticle.getParentResourcePrimKey());
		Assert.assertEquals(
			kbFolder.getKbFolderId(), kbArticle.getKbFolderId());
	}

	@Test
	public void testMoveKBArticleToParentKBFolderInHomeFolder()
		throws Exception {

		KBArticle kbArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			null, _serviceContext);

		KBFolder parentKBFolder = KBFolderLocalServiceUtil.addKBFolder(
			null, _user.getUserId(), _group.getGroupId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			_serviceContext);

		KBArticleLocalServiceUtil.moveKBArticle(
			_user.getUserId(), kbArticle.getResourcePrimKey(),
			_kbFolderClassNameId, parentKBFolder.getKbFolderId(),
			kbArticle.getPriority());

		kbArticle = KBArticleLocalServiceUtil.getLatestKBArticle(
			kbArticle.getResourcePrimKey(), WorkflowConstants.STATUS_ANY);

		Assert.assertEquals(
			_kbFolderClassNameId, kbArticle.getParentResourceClassNameId());
		Assert.assertEquals(
			parentKBFolder.getKbFolderId(),
			kbArticle.getParentResourcePrimKey());
	}

	@Test
	public void testPreviousAndNextKBArticles() throws Exception {
		KBArticle parentKBArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			null, _serviceContext);

		KBArticle childKBArticle1 = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), parentKBArticle.getClassNameId(),
			parentKBArticle.getResourcePrimKey(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);

		KBArticle childKBArticle2 = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), parentKBArticle.getClassNameId(),
			parentKBArticle.getResourcePrimKey(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), null, null, null, _serviceContext);

		KBArticle topLevelKBArticle = KBArticleLocalServiceUtil.addKBArticle(
			null, _user.getUserId(), _kbFolderClassNameId,
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), StringUtil.randomString(),
			StringUtil.randomString(), StringUtil.randomString(), null, null,
			null, _serviceContext);

		KBArticle[] parentPreviousAndNextKBArticles =
			KBArticleLocalServiceUtil.getPreviousAndNextKBArticles(
				parentKBArticle.getKbArticleId());

		Assert.assertNull(parentPreviousAndNextKBArticles[0]);
		Assert.assertEquals(
			childKBArticle1, parentPreviousAndNextKBArticles[2]);

		KBArticle[] child1PreviousAndNextKBArticles =
			KBArticleLocalServiceUtil.getPreviousAndNextKBArticles(
				childKBArticle1.getKbArticleId());

		Assert.assertEquals(
			parentKBArticle, child1PreviousAndNextKBArticles[0]);
		Assert.assertEquals(
			childKBArticle2, child1PreviousAndNextKBArticles[2]);

		KBArticle[] child2PreviousAndNextKBArticles =
			KBArticleLocalServiceUtil.getPreviousAndNextKBArticles(
				childKBArticle2.getKbArticleId());

		Assert.assertEquals(
			childKBArticle1, child2PreviousAndNextKBArticles[0]);
		Assert.assertEquals(
			topLevelKBArticle, child2PreviousAndNextKBArticles[2]);

		KBArticle[] topLevelPreviousAndNextKBArticles =
			KBArticleLocalServiceUtil.getPreviousAndNextKBArticles(
				topLevelKBArticle.getKbArticleId());

		Assert.assertEquals(
			childKBArticle2, topLevelPreviousAndNextKBArticles[0]);
		Assert.assertNull(topLevelPreviousAndNextKBArticles[2]);
	}

	protected void importMarkdownArticles() throws PortalException {
		Class<?> clazz = getClass();

		ClassLoader classLoader = clazz.getClassLoader();

		String fileName = "markdown-articles.zip";

		InputStream zipFileInputStream = classLoader.getResourceAsStream(
			fileName);

		KBArticleLocalServiceUtil.addKBArticlesMarkdown(
			_user.getUserId(), _group.getGroupId(),
			KBFolderConstants.DEFAULT_PARENT_FOLDER_ID, fileName, true,
			zipFileInputStream, _serviceContext);
	}

	protected void updateWorkflowDefinitionForKBArticle(
			String workflowDefinition)
		throws PortalException {

		WorkflowDefinitionLinkLocalServiceUtil.updateWorkflowDefinitionLink(
			_user.getUserId(), _user.getCompanyId(), _group.getGroupId(),
			KBArticle.class.getName(), 0, 0, workflowDefinition);
	}

	private static final Pattern _tagetBlankPattern = Pattern.compile(
		".*target=\"_blank\".*");

	@DeleteAfterTestRun
	private Group _group;

	private long _kbArticleClassNameId;
	private long _kbFolderClassNameId;
	private ServiceContext _serviceContext;
	private User _user;

}