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

package com.liferay.commerce.internal.upgrade.v2_1_0;

import com.liferay.commerce.internal.upgrade.base.BaseCommerceServiceUpgradeProcess;
import com.liferay.commerce.model.impl.CommerceSubscriptionEntryModelImpl;
import com.liferay.commerce.product.model.CPDefinition;
import com.liferay.commerce.product.model.CPInstance;
import com.liferay.commerce.product.service.CPDefinitionLocalService;
import com.liferay.commerce.product.service.CPInstanceLocalService;
import com.liferay.portal.kernel.dao.db.IndexMetadata;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ObjectValuePair;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.List;
import java.util.Objects;

/**
 * @author Ethan Bustad
 */
public class CommerceSubscriptionEntryUpgradeProcess
	extends BaseCommerceServiceUpgradeProcess {

	public CommerceSubscriptionEntryUpgradeProcess(
		CPDefinitionLocalService cpDefinitionLocalService,
		CPInstanceLocalService cpInstanceLocalService) {

		_cpDefinitionLocalService = cpDefinitionLocalService;
		_cpInstanceLocalService = cpInstanceLocalService;
	}

	@Override
	protected void doUpgrade() throws Exception {
		addColumn("CommerceSubscriptionEntry", "CPInstanceUUID", "VARCHAR(75)");
		addColumn("CommerceSubscriptionEntry", "CProductId", "LONG");

		_addIndexes(CommerceSubscriptionEntryModelImpl.TABLE_NAME);

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				"update CommerceSubscriptionEntry set CProductId = ?," +
					"CPInstanceUUID = ? where CPInstanceId = ?");
			Statement s = connection.createStatement();
			ResultSet resultSet = s.executeQuery(
				"select distinct CPInstanceId from " +
					"CommerceSubscriptionEntry")) {

			while (resultSet.next()) {
				long cpInstanceId = resultSet.getLong("CPInstanceId");

				CPInstance cpInstance = _cpInstanceLocalService.getCPInstance(
					cpInstanceId);

				CPDefinition cpDefinition =
					_cpDefinitionLocalService.getCPDefinition(
						cpInstance.getCPDefinitionId());

				preparedStatement.setLong(1, cpDefinition.getCProductId());

				preparedStatement.setString(2, cpInstance.getCPInstanceUuid());

				preparedStatement.setLong(3, cpInstanceId);

				preparedStatement.execute();
			}
		}

		alterTableDropColumn("CommerceSubscriptionEntry", "CPInstanceId");
	}

	private void _addIndexes(String tableName) throws Exception {
		Class<?> clazz = getClass();

		List<ObjectValuePair<String, IndexMetadata>> indexesSQL = getIndexesSQL(
			clazz.getClassLoader(), tableName);

		for (ObjectValuePair<String, IndexMetadata> indexSQL : indexesSQL) {
			IndexMetadata indexMetadata = indexSQL.getValue();

			if (_log.isInfoEnabled()) {
				_log.info(
					String.format(
						"Adding index %s to table %s",
						indexMetadata.getIndexName(), tableName));
			}

			if (!_tableHasIndex(tableName, indexMetadata.getIndexName())) {
				runSQL(indexMetadata.getCreateSQL(null));
			}
			else if (_log.isInfoEnabled()) {
				_log.info(
					String.format(
						"Index %s already exists on table %s",
						indexMetadata.getIndexName(), tableName));
			}
		}
	}

	private boolean _tableHasIndex(String tableName, String indexName)
		throws Exception {

		DatabaseMetaData metadata = connection.getMetaData();

		try (ResultSet resultSet = metadata.getIndexInfo(
				null, null, tableName, false, false)) {

			while (resultSet.next()) {
				String curIndexName = resultSet.getString("index_name");

				if (Objects.equals(indexName, curIndexName)) {
					return true;
				}
			}
		}

		return false;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		CommerceSubscriptionEntryUpgradeProcess.class);

	private final CPDefinitionLocalService _cpDefinitionLocalService;
	private final CPInstanceLocalService _cpInstanceLocalService;

}