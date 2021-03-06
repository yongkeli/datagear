/*
 * Copyright 2018 datagear.tech. All Rights Reserved.
 */

package org.datagear.dbinfo;

import java.sql.Connection;
import java.sql.ResultSetMetaData;

/**
 * 数据库信息解析器。
 * 
 * @author datagear@163.com
 *
 */
public interface DatabaseInfoResolver
{
	/**
	 * 获取{@linkplain DatabaseInfo}。
	 * 
	 * @param cn
	 * @return
	 * @throws DatabaseInfoResolverException
	 */
	DatabaseInfo getDatabaseInfo(Connection cn) throws DatabaseInfoResolverException;

	/**
	 * 获取{@linkplain EntireTableInfo}。
	 * 
	 * @param cn
	 * @param tableName
	 * @return
	 * @throws DatabaseInfoResolverException
	 */
	EntireTableInfo getEntireTableInfo(Connection cn, String tableName) throws DatabaseInfoResolverException;

	/**
	 * 获取所有{@linkplain TableInfo}。
	 * <p>
	 * 此方法应该仅返回{@linkplain TableType#TABLE}、{@linkplain TableType#VIEW}的记录。
	 * </p>
	 * 
	 * @param cn
	 * @return
	 * @throws DatabaseInfoResolverException
	 */
	TableInfo[] getTableInfos(Connection cn) throws DatabaseInfoResolverException;

	/**
	 * 获取一个随机表信息。
	 * <p>
	 * 返回{@linkplain TableInfo#getType()}为{@linkplain TableType#TABLE}。
	 * </p>
	 * <p>
	 * 如果没有任何表，将返回{@code null}。
	 * </p>
	 * 
	 * @param cn
	 * @return
	 * @throws DatabaseInfoResolverException
	 */
	TableInfo getRandomTableInfo(Connection cn) throws DatabaseInfoResolverException;

	/**
	 * 获取指定名称的{@linkplain TableInfo}。
	 * 
	 * @param cn
	 * @param tableName
	 * @return
	 * @throws DatabaseInfoResolverException
	 */
	TableInfo getTableInfo(Connection cn, String tableName) throws DatabaseInfoResolverException;

	/**
	 * 获取指定表的{@linkplain ColumnInfo}。
	 * 
	 * @param cn
	 * @param tableName
	 * @return
	 * @throws DatabaseInfoResolverException
	 */
	ColumnInfo[] getColumnInfos(Connection cn, String tableName) throws DatabaseInfoResolverException;

	/**
	 * 获取指定表的一个随机{@linkplain ColumnInfo}。
	 * <p>
	 * 如果表没有任何字段，将返回{@code null}。
	 * </p>
	 * 
	 * @param cn
	 * @param tableName
	 * @return
	 * @throws DatabaseInfoResolverException
	 */
	ColumnInfo getRandomColumnInfo(Connection cn, String tableName) throws DatabaseInfoResolverException;

	/**
	 * 获取指定{@linkplain ResultSetMetaData}的{@linkplain ColumnInfo}。
	 * 
	 * @param cn
	 * @param resultSetMetaData
	 * @return
	 * @throws DatabaseInfoResolverException
	 */
	ColumnInfo[] getColumnInfos(Connection cn, ResultSetMetaData resultSetMetaData)
			throws DatabaseInfoResolverException;

	/**
	 * 获取指定表的主键列名称。
	 * 
	 * @param cn
	 * @param tableName
	 * @return
	 * @throws DatabaseInfoResolverException
	 */
	String[] getPrimaryKeyColumnNames(Connection cn, String tableName) throws DatabaseInfoResolverException;

	/**
	 * 获取指定表的唯一键列名称。
	 * 
	 * @param cn
	 * @param tableName
	 * @return
	 * @throws DatabaseInfoResolverException
	 */
	String[][] getUniqueKeyColumnNames(Connection cn, String tableName) throws DatabaseInfoResolverException;

	/**
	 * 获取指定表的{@linkplain ImportedKeyInfo}。
	 * 
	 * @param cn
	 * @param tableName
	 * @return
	 * @throws DatabaseInfoResolverException
	 */
	ImportedKeyInfo[] getImportedKeyInfos(Connection cn, String tableName) throws DatabaseInfoResolverException;

	/**
	 * 获取指定表的{@linkplain ExportedKeyInfo}。
	 * 
	 * @param cn
	 * @param tableName
	 * @return
	 * @throws DatabaseInfoResolverException
	 */
	ExportedKeyInfo[] getExportedKeyInfos(Connection cn, String tableName) throws DatabaseInfoResolverException;

	/**
	 * 获取指定表的导入表（外键依赖表）。
	 * <p>
	 * 如果某个索引的表不存在或者没有导入表，返回数组相应位置将是空数组。
	 * </p>
	 * 
	 * @param cn
	 * @param tables
	 * @return
	 * @throws DatabaseInfoResolverException
	 */
	String[][] getImportedTables(Connection cn, String[] tables) throws DatabaseInfoResolverException;

	/**
	 * 获取{@linkplain SqlTypeInfo}。
	 * <p>
	 * 如果无法获取，将返回空数组。
	 * </p>
	 * 
	 * @param cn
	 * @return
	 * @throws DatabaseInfoResolverException
	 */
	SqlTypeInfo[] getSqlTypeInfos(Connection cn) throws DatabaseInfoResolverException;
}
