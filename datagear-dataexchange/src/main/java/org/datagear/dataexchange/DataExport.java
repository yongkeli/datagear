/*
 * Copyright (c) 2018 datagear.org. All Rights Reserved.
 */

package org.datagear.dataexchange;

import javax.sql.DataSource;

/**
 * 导出端。
 * 
 * @author datagear@163.com
 *
 */
public abstract class DataExport extends DataExchange
{
	private DataExportReporter dataExportReporter;

	public DataExport()
	{
		super();
	}

	public DataExport(DataSource dataSource, boolean abortOnError)
	{
		super(dataSource, abortOnError);
	}

	public boolean hasDataExportReporter()
	{
		return (this.dataExportReporter != null);
	}

	public DataExportReporter getDataExportReporter()
	{
		return dataExportReporter;
	}

	public void setDataExportReporter(DataExportReporter dataExportReporter)
	{
		this.dataExportReporter = dataExportReporter;
	}
}
