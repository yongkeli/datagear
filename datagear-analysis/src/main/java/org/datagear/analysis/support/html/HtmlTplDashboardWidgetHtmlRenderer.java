/*
 * Copyright (c) 2018 datagear.tech. All Rights Reserved.
 */

/**
 * 
 */
package org.datagear.analysis.support.html;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.datagear.analysis.Chart;
import org.datagear.analysis.RenderException;
import org.datagear.analysis.support.ChartWidgetSource;
import org.datagear.analysis.support.DashboardWidgetResManager;
import org.datagear.util.StringUtil;

/**
 * 使用原生HTML网页作为模板的{@linkplain HtmlTplDashboardWidget}渲染器。
 * <p>
 * 此类可渲染由{@linkplain DashboardWidgetResManager}管理模板的{@linkplain HtmlTplDashboardWidget}，
 * 其中{@linkplain HtmlTplDashboardWidget#getTemplate()}应该是可以通过{@linkplain DashboardWidgetResManager#getFile(String, String)}找到的模板文件名。
 * </p>
 * <p>
 * 支持的模板格式如下：
 * </p>
 * <code>
 * <pre>
 * ...
 * &lt;html dg-dashboard-renderer="..." dg-dashboard-var="..." dg-dashboard-import-exclude="..."&gt;
 * ...
 * &lt;head&gt;
 * ...
 * &lt;/head&gt;
 * ...
 * &lt;body&gt;
 * ...
 * &lt;div id="..." dg-chart-widget="..."&gt;&lt;/div&gt;
 * ...
 * &lt;/body&gt;
 * &lt;/html&gt;
 * </pre>
 * </code>
 * <p>
 * <code>html dg-dashboard-renderer</code>：选填，定义看板渲染器JS对象的变量名，默认为{@linkplain HtmlTplDashboardWidgetRenderer#getDefaultDashboardRendererVar()}
 * </p>
 * <p>
 * <code>html dg-dashboard-var</code>：选填，定义看板JS对象的变量名，默认为{@linkplain #getDefaultDashboardVar()}
 * </p>
 * <p>
 * <code>html dg-dashboard-import-exclude</code>：选填，定义看板网页不加载的内置库（{@linkplain HtmlTplDashboardWidgetRenderer#getDashboardImports()}），多个以“,”隔开
 * </p>
 * <p>
 * <code>div id</code>：选填，定义图表元素ID，如果不填，则会自动生成一个
 * </p>
 * <p>
 * <code>div dg-chart-widget</code>：必填，定义图表部件ID（{@linkplain HtmlChartWidget#getId()}）
 * </p>
 * 
 * @author datagear@163.com
 *
 * @param <T>
 */
public class HtmlTplDashboardWidgetHtmlRenderer<T extends HtmlRenderContext> extends HtmlTplDashboardWidgetRenderer<T>
{
	public static final String DEFAULT_DASHBOARD_SET_TAG_NAME = "html";

	public static final String DEFAULT_CHART_TAG_NAME = "div";

	public static final String DEFAULT_ATTR_NAME_DASHBOARD_VAR = "dg-dashboard-var";

	public static final String DEFAULT_ATTR_NAME_DASHBOARD_RENDERER = "dg-dashboard-renderer";

	public static final String DEFAULT_ATTR_NAME_DASHBOARD_IMPORT_EXCLUDE = "dg-dashboard-import-exclude";

	public static final String DEFAULT_ATTR_NAME_CHART_WIDGET = "dg-chart-widget";

	public static final String DEFAULT_DASHBOARD_VAR = "dashboard";

	/** 看板设置标签名 */
	private String dashboardSetTagName = DEFAULT_DASHBOARD_SET_TAG_NAME;

	/** 属性名：看板JS变量名 */
	private String attrNameDashboardVar = DEFAULT_ATTR_NAME_DASHBOARD_VAR;

	/** 属性名：看板渲染器JS变量名 */
	private String attrNameDashboardRenderer = DEFAULT_ATTR_NAME_DASHBOARD_RENDERER;

	/** 属性名：不导入内置库的 */
	private String attrNameDashboardImportExclude = DEFAULT_ATTR_NAME_DASHBOARD_IMPORT_EXCLUDE;

	/** 图表标签名 */
	private String chartTagName = DEFAULT_CHART_TAG_NAME;

	/** 属性名：图表部件ID */
	private String attrNameChartWidget = DEFAULT_ATTR_NAME_CHART_WIDGET;

	/** 默认看板变量名 */
	private String defaultDashboardVar = DEFAULT_DASHBOARD_VAR;

	public HtmlTplDashboardWidgetHtmlRenderer()
	{
		super();
	}

	public HtmlTplDashboardWidgetHtmlRenderer(DashboardWidgetResManager dashboardWidgetResManager,
			ChartWidgetSource chartWidgetSource)
	{
		super(dashboardWidgetResManager, chartWidgetSource);
	}

	public String getDashboardSetTagName()
	{
		return dashboardSetTagName;
	}

	public void setDashboardSetTagName(String dashboardSetTagName)
	{
		this.dashboardSetTagName = dashboardSetTagName;
	}

	public String getAttrNameDashboardVar()
	{
		return attrNameDashboardVar;
	}

	public void setAttrNameDashboardVar(String attrNameDashboardVar)
	{
		this.attrNameDashboardVar = attrNameDashboardVar;
	}

	public String getAttrNameDashboardRenderer()
	{
		return attrNameDashboardRenderer;
	}

	public void setAttrNameDashboardRenderer(String attrNameDashboardRenderer)
	{
		this.attrNameDashboardRenderer = attrNameDashboardRenderer;
	}

	public String getAttrNameDashboardImportExclude()
	{
		return attrNameDashboardImportExclude;
	}

	public void setAttrNameDashboardImportExclude(String attrNameDashboardImportExclude)
	{
		this.attrNameDashboardImportExclude = attrNameDashboardImportExclude;
	}

	public String getChartTagName()
	{
		return chartTagName;
	}

	public void setChartTagName(String chartTagName)
	{
		this.chartTagName = chartTagName;
	}

	public String getAttrNameChartWidget()
	{
		return attrNameChartWidget;
	}

	public void setAttrNameChartWidget(String attrNameChartWidget)
	{
		this.attrNameChartWidget = attrNameChartWidget;
	}

	public String getDefaultDashboardVar()
	{
		return defaultDashboardVar;
	}

	public void setDefaultDashboardVar(String defaultDashboardVar)
	{
		this.defaultDashboardVar = defaultDashboardVar;
	}

	@Override
	protected void renderHtmlDashboard(T renderContext, HtmlDashboard dashboard) throws Throwable
	{
		HtmlTplDashboardWidget<?> dashboardWidget = (HtmlTplDashboardWidget<?>) dashboard.getWidget();
		Reader in = getTemplateReaderNotNull(dashboardWidget);

		renderHtmlDashboard(renderContext, dashboard, in);
	}

	/**
	 * 渲染{@linkplain HtmlDashboard}。
	 * 
	 * @param renderContext
	 * @param dashboard
	 * @param in
	 * @return
	 * @throws Exception
	 */
	protected DashboardInfo renderHtmlDashboard(T renderContext, HtmlDashboard dashboard, Reader in) throws Exception
	{
		Writer out = renderContext.getWriter();

		DashboardInfo dashboardInfo = new DashboardInfo();
		boolean wroteDashboard = false;

		StringBuilder cache = new StringBuilder();
		StringBuilder nameCache = new StringBuilder();
		StringBuilder valueCache = new StringBuilder();

		int c = -1;
		while ((c = in.read()) > -1)
		{
			if (c == '<')
			{
				if (isNotEmpty(cache))
					clear(cache);
				if (isNotEmpty(nameCache))
					clear(nameCache);
				if (isNotEmpty(valueCache))
					clear(valueCache);

				appendChar(cache, c);

				int last = skipWhitespace(in, cache);

				if (last < 0)
				{
					out.write(cache.toString());
				}
				// </...
				else if (last == '/')
				{
					if (wroteDashboard)
						;
					else
					{
						last = resolveCloseTagName(in, cache, nameCache);

						if (isNotEmpty(nameCache))
						{
							String tagName = nameCache.toString();

							// </body>前写入看板脚本
							if ("body".equalsIgnoreCase(tagName))
							{
								writeHtmlDashboardScript(renderContext, dashboard, dashboardInfo);
								wroteDashboard = true;
							}
						}
					}

					out.write(cache.toString());
				}
				// <>
				else if (last == '>')
				{
					out.write(cache.toString());
				}
				// <...
				else
				{
					appendChar(nameCache, last);

					last = resolveTagName(in, cache, nameCache);

					String tagName = nameCache.toString();

					if (this.dashboardSetTagName.equalsIgnoreCase(tagName))
					{
						clear(nameCache);
						last = resolveDashboardInfo(in, last, cache, nameCache, valueCache, dashboardInfo);

						out.write(cache.toString());
					}
					else if ("head".equalsIgnoreCase(tagName))
					{
						clear(nameCache);

						for (;;)
						{
							last = resolveTagAttr(in, last, cache, nameCache, valueCache);
							if (isTagEnd(last))
								break;
						}

						out.write(cache.toString());
						writeDashboardImport(renderContext, dashboard, dashboardInfo);
					}
					else if (this.chartTagName.equalsIgnoreCase(tagName))
					{
						clear(nameCache);

						last = resolveDashboardChartInfo(renderContext, dashboard, in, last, cache, nameCache,
								valueCache, dashboardInfo);

						out.write(cache.toString());
					}
					else
						out.write(cache.toString());
				}
			}
			else
				out.write(c);
		}

		return dashboardInfo;
	}

	/**
	 * 写看板导入项。
	 * 
	 * @param renderContext
	 * @param dashboard
	 * @param dashboardInfo
	 * @throws IOException
	 */
	protected void writeDashboardImport(T renderContext, HtmlDashboard dashboard, DashboardInfo dashboardInfo)
			throws IOException
	{
		writeDashboardImport(renderContext, dashboard, dashboardInfo.getImportExclude());
	}

	/**
	 * 写HTML看板脚本。
	 * 
	 * @param renderContext
	 * @param dashboard
	 * @param dashboardInfo
	 * @throws IOException
	 */
	protected void writeHtmlDashboardScript(T renderContext, HtmlDashboard dashboard, DashboardInfo dashboardInfo)
			throws IOException
	{
		String dashboardVar = dashboardInfo.getDashboardVar();
		if (StringUtil.isEmpty(dashboardVar))
			dashboardVar = this.defaultDashboardVar;

		dashboard.setVarName(dashboardVar);

		Writer out = renderContext.getWriter();

		writeScriptStartTag(out);
		writeNewLine(out);

		writeHtmlDashboardJSVar(out, dashboard, true);

		out.write("(function(){");
		writeNewLine(out);

		HtmlRenderAttributes.setChartRenderContextVarName(renderContext, dashboardVar + ".renderContext");

		List<ChartInfo> chartInfos = dashboardInfo.getChartInfos();
		if (chartInfos != null)
		{
			for (ChartInfo chartInfo : chartInfos)
				writeHtmlChartScript(renderContext, dashboard, dashboardInfo, chartInfo);

			// 移除内部设置的属性
			HtmlRenderAttributes.removeChartRenderContextVarName(renderContext);
			HtmlRenderAttributes.removeChartNotRenderScriptTag(renderContext);
			HtmlRenderAttributes.removeChartScriptNotInvokeRender(renderContext);
			HtmlRenderAttributes.removeChartNotRenderElement(renderContext);
			HtmlRenderAttributes.removeChartVarName(renderContext);
			HtmlRenderAttributes.removeChartElementId(renderContext);
			renderContext.removeAttribute(RENDER_ATTR_NAME_FOR_NOT_FOUND_SCRIPT);
		}

		String tmpRenderContextVar = HtmlRenderAttributes
				.generateRenderContextVarName(HtmlRenderAttributes.getNextSequenceIfNot(renderContext, -1));

		writeHtmlDashboardJSInit(out, dashboard, tmpRenderContextVar);

		out.write("})();");
		writeNewLine(out);

		writeHtmlDashboardJSRender(out, dashboard, dashboardInfo.getRendererVar());

		writeScriptEndTag(out);
		writeNewLine(out);
	}

	/**
	 * 写HTML图表脚本内容。
	 * 
	 * @param renderContext
	 * @param dashboard
	 * @param dashboardInfo
	 * @param chartInfo
	 * @throws IOException
	 */
	protected void writeHtmlChartScript(T renderContext, HtmlDashboard dashboard, DashboardInfo dashboardInfo,
			ChartInfo chartInfo) throws IOException
	{
		String widget = chartInfo.getWidgetId();
		String elementId = chartInfo.getElementId();
		String var = null;

		if (StringUtil.isEmpty(elementId))
			throw new RenderException("The 'id' attribute must be set for chart element");

		int nextSequence = -1;

		HtmlChartWidget<HtmlRenderContext> chartWidget = getHtmlChartWidgetForRender(renderContext, widget);

		if (StringUtil.isEmpty(var))
		{
			nextSequence = HtmlRenderAttributes.getNextSequenceIfNot(renderContext, nextSequence);
			var = HtmlRenderAttributes.generateChartVarName(nextSequence);
		}

		HtmlRenderAttributes.setChartNotRenderScriptTag(renderContext, true);
		HtmlRenderAttributes.setChartNotRenderElement(renderContext, true);
		HtmlRenderAttributes.setChartScriptNotInvokeRender(renderContext, true);
		HtmlRenderAttributes.setChartVarName(renderContext, var);
		HtmlRenderAttributes.setChartElementId(renderContext, elementId);

		HtmlChart chart = chartWidget.render(renderContext);

		List<Chart> charts = dashboard.getCharts();
		if (charts == null)
		{
			charts = new ArrayList<Chart>();
			dashboard.setCharts(charts);
		}

		charts.add(chart);
	}

	/**
	 * 解析{@linkplain DashboardInfo#getImportExclude()}。
	 * 
	 * @param in
	 * @param last
	 * @param cache
	 * @param attrName
	 * @param attrValue
	 * @param dashboardInfo
	 * @return
	 * @throws IOException
	 */
	protected int resolveDashboardInfo(Reader in, int last, StringBuilder cache, StringBuilder attrName,
			StringBuilder attrValue, DashboardInfo dashboardInfo) throws IOException
	{
		for (;;)
		{
			last = resolveTagAttr(in, last, cache, attrName, attrValue);

			String attrNameStr = attrName.toString();

			if (this.attrNameDashboardVar.equalsIgnoreCase(attrNameStr))
			{
				dashboardInfo.setDashboardVar(attrValue.toString().trim());
			}
			else if (this.attrNameDashboardRenderer.equalsIgnoreCase(attrNameStr))
			{
				dashboardInfo.setRendererVar(attrValue.toString().trim());
			}
			else if (this.attrNameDashboardImportExclude.equalsIgnoreCase(attrNameStr))
			{
				dashboardInfo.setImportExclude(attrValue.toString().trim());
			}

			clear(attrName);
			clear(attrValue);

			if (isTagEnd(last))
				break;
		}

		return last;
	}

	/**
	 * 解析{@linkplain DashboardInfo#getChartInfos()}。
	 * 
	 * @param renderContext
	 * @param dashboard
	 * @param in
	 * @param last
	 * @param cache
	 * @param attrName
	 * @param attrValue
	 * @param dashboardInfo
	 * @return
	 * @throws IOException
	 */
	protected int resolveDashboardChartInfo(T renderContext, HtmlDashboard dashboard, Reader in, int last,
			StringBuilder cache, StringBuilder attrName, StringBuilder attrValue, DashboardInfo dashboardInfo)
			throws IOException
	{
		ChartInfo chartInfo = null;

		for (;;)
		{
			last = resolveTagAttr(in, last, cache, attrName, attrValue);

			String attrNameStr = attrName.toString();

			if (this.attrNameChartWidget.equalsIgnoreCase(attrNameStr))
			{
				if (chartInfo == null)
					chartInfo = new ChartInfo();

				chartInfo.setWidgetId(attrValue.toString().trim());
			}
			else if ("id".equalsIgnoreCase(attrNameStr))
			{
				if (chartInfo == null)
					chartInfo = new ChartInfo();

				chartInfo.setElementId(attrValue.toString().trim());
			}

			clear(attrName);
			clear(attrValue);

			if (isTagEnd(last))
				break;
		}

		if (chartInfo != null && StringUtil.isEmpty(chartInfo.getWidgetId()))
			chartInfo = null;

		// 元素没有定义“id”属性
		if (chartInfo != null && StringUtil.isEmpty(chartInfo.getElementId()))
		{
			String elementId = HtmlRenderAttributes
					.generateChartElementId(HtmlRenderAttributes.getNextSequenceIfNot(renderContext, -1));

			chartInfo.setElementId(elementId);

			int insertIdx = findInsertAttrIndex(cache);
			cache.insert(insertIdx, " id=\"" + elementId + "\" ");
		}

		if (chartInfo != null)
			dashboardInfo.addChartInfo(chartInfo);

		return last;
	}

	/**
	 * 查找可以插入属性的位置。
	 * 
	 * @param sb
	 * @return
	 */
	protected int findInsertAttrIndex(StringBuilder sb)
	{
		int i = sb.length() - 1;
		for (; i >= 0; i--)
		{
			int c = sb.charAt(i);

			if (isWhitespace(c))
				continue;
			else if (c != '>' && c != '/')
			{
				i = i + 1;
				break;
			}
		}

		return i;
	}

	/**
	 * 解析标签名。
	 * 
	 * @param in
	 * @param cache
	 *            读取字符缓存
	 * @param tagName
	 * @return '>'、'/'、空格、-1
	 * @throws IOException
	 */
	protected int resolveTagName(Reader in, StringBuilder cache, StringBuilder tagName) throws IOException
	{
		int c = -1;
		while ((c = in.read()) > -1)
		{
			appendChar(cache, c);

			if (c == '>' || c == '/')
			{
				break;
			}
			else if (isWhitespace(c))
			{
				if (isNotEmpty(tagName))
					break;
			}
			else
				appendChar(tagName, c);
		}

		return c;
	}

	/**
	 * 解析标签属性。
	 * 
	 * @param in
	 * @param last
	 *            上一个已读取的字符
	 * @param cache
	 * @param attrName
	 * @param attrValue
	 * @return '>'、'/'、空格、下一个属性名的第一个字符、-1
	 * @throws IOException
	 */
	protected int resolveTagAttr(Reader in, int last, StringBuilder cache, StringBuilder attrName,
			StringBuilder attrValue) throws IOException
	{
		// 上一个字符是标签结束字符
		if (isTagEnd(last))
			return last;

		// 上一个字符是此属性名的第一个字符
		if (last != '/' && !isWhitespace(last))
			appendChar(attrName, last);

		boolean resolveAttValue = false;

		int c = -1;
		while ((c = in.read()) > -1)
		{
			appendChar(cache, c);

			if (c == '>' || c == '/')
			{
				break;
			}
			else if (c == '=')
			{
				if (isNotEmpty(attrName))
					resolveAttValue = true;
				else
					appendChar(attrName, c);
			}
			else if (c == '\'' || c == '"')
			{
				if (!resolveAttValue)
					appendChar(attrName, c);
				else
				{
					if (!isEmpty(attrValue))
						appendChar(attrValue, c);
					else
					{
						boolean endQuote = false;
						int quote = c;

						while ((c = in.read()) > -1)
						{
							appendChar(cache, c);

							if (c == quote)
							{
								c = in.read();
								appendChar(cache, c);

								endQuote = true;
								break;
							}
							else
								appendChar(attrValue, c);
						}

						if (endQuote)
							break;
					}
				}
			}
			else if (isWhitespace(c))
			{
				if (isNotEmpty(attrValue))
					break;
			}
			else
			{
				if (resolveAttValue)
					appendChar(attrValue, c);
				else
				{
					int prev = (isEmpty(cache) ? 0 : cache.charAt(cache.length() - 1));

					// 只有属性名没有属性值
					if (isWhitespace(prev))
						break;
					else
						appendChar(attrName, c);
				}
			}
		}

		return c;
	}

	/**
	 * 解析关闭标签名（“&lt;/tag-name&gt;”）。
	 * <p>
	 * 如果不是合法的关闭标签，{@code tagName}将为空。
	 * </p>
	 * 
	 * @param in
	 * @param cache
	 * @param tagName
	 * @return
	 * @throws IOException
	 */
	protected int resolveCloseTagName(Reader in, StringBuilder cache, StringBuilder tagName) throws IOException
	{
		int last = resolveTagName(in, cache, tagName);

		if (Character.isWhitespace(last))
			last = skipWhitespace(in, cache);

		if (isTagEnd(last))
			;
		else
			clear(tagName);

		return last;
	}

	/**
	 * 给定字符是否是标签结束符。
	 * 
	 * @param c
	 * @return
	 */
	protected boolean isTagEnd(int c)
	{
		return (c == '>' || c < 0);
	}

	/**
	 * {@linkplain StringBuilder}是否为空。
	 * 
	 * @param sb
	 * @return
	 */
	protected boolean isEmpty(StringBuilder sb)
	{
		return (sb == null || sb.length() == 0);
	}

	/**
	 * {@linkplain StringBuilder}是否不为空。
	 * 
	 * @param sb
	 * @return
	 */
	protected boolean isNotEmpty(StringBuilder sb)
	{
		return (sb != null && sb.length() > 0);
	}

	/**
	 * 清除{@linkplain StringBuilder}。
	 * 
	 * @param sb
	 */
	protected void clear(StringBuilder sb)
	{
		if (sb == null)
			return;

		sb.delete(0, sb.length());
	}

	/**
	 * 跳过空格。
	 * 
	 * @param in
	 * @param cache
	 *            读取字符缓存
	 * @return 非空格字符、-1
	 * @throws IOException
	 */
	protected int skipWhitespace(Reader in, StringBuilder cache) throws IOException
	{
		int c = -1;

		while ((c = in.read()) > -1)
		{
			appendChar(cache, c);

			if (!isWhitespace(c))
				break;
		}

		return c;
	}

	/**
	 * 作为字符追加。
	 * 
	 * @param sb
	 * @param c
	 */
	protected void appendChar(StringBuilder sb, int c)
	{
		sb.appendCodePoint(c);
	}

	/**
	 * 是否空格字符。
	 * 
	 * @param c
	 * @return
	 */
	protected boolean isWhitespace(int c)
	{
		return Character.isWhitespace(c);
	}

	protected static class DashboardInfo
	{
		/** 看板变量名称 */
		private String dashboardVar;
		/** 看板渲染器名称 */
		private String rendererVar;
		/** 内置导入排除项 */
		private String importExclude;
		/** 图表信息 */
		private List<ChartInfo> chartInfos = new ArrayList<ChartInfo>();

		public DashboardInfo()
		{
			super();
		}

		public DashboardInfo(String dashboardVar)
		{
			super();
			this.dashboardVar = dashboardVar;
		}

		public String getDashboardVar()
		{
			return dashboardVar;
		}

		public void setDashboardVar(String dashboardVar)
		{
			this.dashboardVar = dashboardVar;
		}

		public String getRendererVar()
		{
			return rendererVar;
		}

		public void setRendererVar(String rendererVar)
		{
			this.rendererVar = rendererVar;
		}

		public String getImportExclude()
		{
			return importExclude;
		}

		public void setImportExclude(String importExclude)
		{
			this.importExclude = importExclude;
		}

		public List<ChartInfo> getChartInfos()
		{
			return chartInfos;
		}

		public void addChartInfo(ChartInfo chartInfo)
		{
			this.chartInfos.add(chartInfo);
		}

		@Override
		public String toString()
		{
			return getClass().getSimpleName() + " [dashboardVar=" + dashboardVar + ", rendererVar=" + rendererVar
					+ ", importExclude=" + importExclude + ", chartInfos=" + chartInfos + "]";
		}
	}

	protected static class ChartInfo
	{
		/** 图表部件ID */
		private String widgetId;
		/** 图标元素ID */
		private String elementId;

		public ChartInfo()
		{
			super();
		}

		public ChartInfo(String widgetId, String elementId)
		{
			super();
			this.widgetId = widgetId;
			this.elementId = elementId;
		}

		public String getWidgetId()
		{
			return widgetId;
		}

		public void setWidgetId(String widgetId)
		{
			this.widgetId = widgetId;
		}

		public String getElementId()
		{
			return elementId;
		}

		public void setElementId(String elementId)
		{
			this.elementId = elementId;
		}

		@Override
		public String toString()
		{
			return getClass().getSimpleName() + " [widgetId=" + widgetId + ", elementId=" + elementId + "]";
		}
	}
}
