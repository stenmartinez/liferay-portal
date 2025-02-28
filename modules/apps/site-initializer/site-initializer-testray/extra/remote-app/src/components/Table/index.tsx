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

import {ClayCheckbox} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import ClayTable from '@clayui/table';
import classNames from 'classnames';
import {useState} from 'react';
import {useNavigate} from 'react-router-dom';

import {Sort} from '../../context/ListViewContext';
import {SortDirection, SortOption} from '../../types';
import DropDown from '../DropDown/DropDown';

const {Body, Cell, Head, Row} = ClayTable;

type Column<T = any> = {
	clickable?: boolean;
	key: string;
	render?: (itemValue: any, item: T) => String | React.ReactNode;
	size?: 'sm' | 'md' | 'lg' | 'xl' | 'none';
	sorteable?: boolean;
	value: string;
};

export type TableProps<T = any> = {
	actions?: any[];
	columns: Column[];
	items: T[];
	navigateTo?: (item: T) => string;
	onSelectAllRows: () => void;
	onSelectRow?: (row: any) => void;
	onSort: (columnTable: string, direction: SortDirection) => void;
	rowSelectable?: boolean;
	selectedRows?: number[];
	sort?: Sort;
};

const Table: React.FC<TableProps> = ({
	actions,
	columns,
	items,
	navigateTo,
	onSelectRow,
	onSelectAllRows,
	onSort,
	rowSelectable = false,
	selectedRows = [],
	sort,
}) => {
	const [activeRow, setActiveRow] = useState<number | undefined>();
	const [checked, setChecked] = useState(false);
	const [sorted, setSorted] = useState<SortDirection>(SortOption.ASC);

	const displayActionColumn = !!actions?.length;

	const navigate = useNavigate();

	const onMouseLeaveRow = () => {
		if (displayActionColumn) {
			setActiveRow(undefined);
		}
	};

	const onMouseOverRow = (rowIndex: number) => {
		if (displayActionColumn) {
			setActiveRow(rowIndex);
		}
	};

	function changeSort(key: string) {
		onSort(key, sorted);
		setSorted(
			sorted === SortOption.DESC ? SortOption.ASC : SortOption.DESC
		);
	}

	const getSortSymbol = (key: string) => {
		if (!sort) {
			return '';
		}

		if (sort.key === key) {
			return sort.direction === SortOption.ASC
				? 'caret-top-l'
				: 'caret-bottom-l';
		}

		return 'caret-double-l';
	};

	return (
		<ClayTable borderless className="testray-table" hover>
			<Head className="testray-table">
				<Row>
					{rowSelectable && (
						<Cell>
							<ClayCheckbox
								checked={checked}
								onChange={() => {
									onSelectAllRows();
									setChecked(!checked);
								}}
							/>
						</Cell>
					)}

					{columns.map((column, index) => (
						<Cell
							className="align-items-center text-nowrap"
							headingTitle
							key={index}
						>
							<>
								{column.value}

								{column.sorteable && (
									<ClayIcon
										className="cursor-pointer ml-1"
										onClick={() => changeSort(column.key)}
										symbol={getSortSymbol(column.key)}
									/>
								)}
							</>
						</Cell>
					))}

					{displayActionColumn && <Cell headingCell />}
				</Row>
			</Head>

			<Body>
				{items.map((item, rowIndex) => (
					<Row
						className="table-row text-nowrap"
						key={rowIndex}
						onMouseLeave={onMouseLeaveRow}
						onMouseOver={() => onMouseOverRow(rowIndex)}
					>
						{rowSelectable && onSelectRow && (
							<Cell>
								<ClayCheckbox
									checked={selectedRows.includes(item.id)}
									onChange={() => onSelectRow(item)}
								/>
							</Cell>
						)}

						{columns.map((column, columnIndex) => (
							<Cell
								className={classNames('text-dark', {
									'cursor-pointer': column.clickable,
									'table-cell-expand': column.size === 'sm',
									'table-cell-expand-small':
										column.size === 'xl',
									'table-cell-expand-smaller':
										column.size === 'lg',
									'table-cell-expand-smallest':
										column.size === 'md',
								})}
								key={columnIndex}
								onClick={() => {
									if (navigateTo && column.clickable) {
										navigate(navigateTo(item));
									}
								}}
							>
								{column.render
									? column.render(item[column.key], {
											...item,
											rowIndex,
									  })
									: item[column.key]}
							</Cell>
						))}

						{displayActionColumn && (
							<Cell
								align="right"
								className="py-0 table-action-column table-cell-expand"
							>
								{activeRow === rowIndex ? (
									<DropDown actions={actions} item={item} />
								) : (
									<div></div>
								)}
							</Cell>
						)}
					</Row>
				))}
			</Body>
		</ClayTable>
	);
};

export default Table;
