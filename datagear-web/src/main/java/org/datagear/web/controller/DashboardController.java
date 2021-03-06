/*
 * Copyright 2018 datagear.tech. All Rights Reserved.
 */

package org.datagear.web.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.datagear.analysis.Chart;
import org.datagear.analysis.ChartTheme;
import org.datagear.analysis.DashboardTheme;
import org.datagear.analysis.DataSetResult;
import org.datagear.analysis.RenderStyle;
import org.datagear.analysis.TemplateDashboardWidgetResManager;
import org.datagear.analysis.support.ChartWidget;
import org.datagear.analysis.support.ChartWidgetSource;
import org.datagear.analysis.support.html.HtmlChartPluginRenderOption;
import org.datagear.analysis.support.html.HtmlRenderAttributes;
import org.datagear.analysis.support.html.HtmlRenderContext;
import org.datagear.analysis.support.html.HtmlRenderContext.WebContext;
import org.datagear.analysis.support.html.HtmlTplDashboard;
import org.datagear.analysis.support.html.HtmlTplDashboardWidget;
import org.datagear.analysis.support.html.HtmlTplDashboardWidgetRenderer;
import org.datagear.analysis.support.html.HtmlTplDashboardWidgetRenderer.AddPrefixHtmlTitleHandler;
import org.datagear.management.domain.HtmlTplDashboardWidgetEntity;
import org.datagear.management.domain.User;
import org.datagear.management.service.HtmlChartWidgetEntityService.ChartWidgetSourceContext;
import org.datagear.management.service.HtmlTplDashboardWidgetEntityService;
import org.datagear.persistence.PagingData;
import org.datagear.util.FileUtil;
import org.datagear.util.IDUtil;
import org.datagear.util.IOUtil;
import org.datagear.util.StringUtil;
import org.datagear.web.OperationMessage;
import org.datagear.web.util.WebUtils;
import org.datagear.web.vo.DataFilterPagingQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

/**
 * 看板控制器。
 * 
 * @author datagear@163.com
 *
 */
@Controller
@RequestMapping("/analysis/dashboard")
public class DashboardController extends AbstractDataAnalysisController implements ServletContextAware
{
	static
	{
		AuthorizationResourceMetas.registerForShare(HtmlTplDashboardWidgetEntity.AUTHORIZATION_RESOURCE_TYPE,
				"dashboard");
	}

	@Autowired
	private HtmlTplDashboardWidgetEntityService htmlTplDashboardWidgetEntityService;

	@Autowired
	private File tempDirectory;

	private ServletContext servletContext;

	public DashboardController()
	{
		super();
	}

	public HtmlTplDashboardWidgetEntityService getHtmlTplDashboardWidgetEntityService()
	{
		return htmlTplDashboardWidgetEntityService;
	}

	public void setHtmlTplDashboardWidgetEntityService(
			HtmlTplDashboardWidgetEntityService htmlTplDashboardWidgetEntityService)
	{
		this.htmlTplDashboardWidgetEntityService = htmlTplDashboardWidgetEntityService;
	}

	public File getTempDirectory()
	{
		return tempDirectory;
	}

	public void setTempDirectory(File tempDirectory)
	{
		this.tempDirectory = tempDirectory;
	}

	public ServletContext getServletContext()
	{
		return servletContext;
	}

	@Override
	public void setServletContext(ServletContext servletContext)
	{
		this.servletContext = servletContext;
	}

	@RequestMapping("/add")
	public String add(HttpServletRequest request, org.springframework.ui.Model model)
	{
		HtmlTplDashboardWidgetEntity dashboard = new HtmlTplDashboardWidgetEntity();

		dashboard.setTemplates(HtmlTplDashboardWidgetEntity.DEFAULT_TEMPLATES);
		dashboard.setTemplateEncoding(HtmlTplDashboardWidget.DEFAULT_TEMPLATE_ENCODING);

		HtmlTplDashboardWidgetRenderer<HtmlRenderContext> renderer = getHtmlTplDashboardWidgetEntityService()
				.getHtmlTplDashboardWidgetRenderer();

		String templateContent = renderer.simpleTemplateContent(dashboard.getTemplateEncoding());

		model.addAttribute("dashboard", dashboard);
		model.addAttribute("templates", toWriteJsonTemplateModel(dashboard.getTemplates()));
		model.addAttribute("templateName", dashboard.getFirstTemplate());
		model.addAttribute("templateContent", templateContent);
		model.addAttribute(KEY_TITLE_MESSAGE_KEY, "dashboard.addDashboard");
		model.addAttribute(KEY_FORM_ACTION, "save");

		return "/analysis/dashboard/dashboard_form";
	}

	@RequestMapping("/edit")
	public String edit(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model,
			@RequestParam("id") String id) throws Exception
	{
		User user = WebUtils.getUser(request, response);

		HtmlTplDashboardWidgetEntity dashboard = this.htmlTplDashboardWidgetEntityService.getByIdForEdit(user, id);

		if (dashboard == null)
			throw new RecordNotFoundException();

		model.addAttribute("dashboard", dashboard);
		model.addAttribute("templates", toWriteJsonTemplateModel(dashboard.getTemplates()));
		model.addAttribute("templateName", dashboard.getFirstTemplate());
		model.addAttribute("templateContent", readTemplateContent(dashboard, dashboard.getFirstTemplate()));
		model.addAttribute(KEY_TITLE_MESSAGE_KEY, "dashboard.editDashboard");
		model.addAttribute(KEY_FORM_ACTION, "save");

		return "/analysis/dashboard/dashboard_form";
	}

	@RequestMapping(value = "/save", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> save(HttpServletRequest request, HttpServletResponse response,
			HtmlTplDashboardWidgetEntity dashboard, @RequestParam("templateName") String templateName,
			@RequestParam("templateContent") String templateContent) throws Exception
	{
		templateName = trimResourceName(templateName);

		User user = WebUtils.getUser(request, response);

		if (!dashboard.isTemplate(templateName))
		{
			List<String> templates = new ArrayList<>();

			if (!isEmpty(dashboard.getTemplates()))
				templates.addAll(Arrays.asList(dashboard.getTemplates()));

			templates.add(templateName);

			dashboard.setTemplates(templates.toArray(new String[templates.size()]));
			trimResourceNames(dashboard.getTemplates());
		}

		checkSaveEntity(dashboard);

		boolean save = false;

		if (templateName.equals(dashboard.getFirstTemplate()))
			dashboard.setTemplateEncoding(resolveTemplateEncoding(templateContent));

		if (isEmpty(dashboard.getId()))
		{
			dashboard.setId(IDUtil.randomIdOnTime20());
			dashboard.setCreateUser(user);
			save = this.htmlTplDashboardWidgetEntityService.add(user, dashboard);
		}
		else
		{
			save = this.htmlTplDashboardWidgetEntityService.update(user, dashboard);
		}

		if (save)
			saveTemplateContent(dashboard, templateName, templateContent);

		Map<String, Object> data = new HashMap<>();
		data.put("id", dashboard.getId());
		data.put("templateName", templateName);
		data.put("templates", dashboard.getTemplates());

		ResponseEntity<OperationMessage> responseEntity = buildOperationMessageSaveSuccessResponseEntity(request);
		responseEntity.getBody().setData(data);

		return responseEntity;
	}

	@RequestMapping(value = "/saveTemplateNames", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveTemplateNames(HttpServletRequest request, HttpServletResponse response,
			org.springframework.ui.Model model, @RequestParam("id") String id,
			@RequestParam(value = "templates", required = false) String[] templates) throws Exception
	{
		if (isEmpty(templates))
			return buildOperationMessageFailResponseEntity(request, HttpStatus.BAD_REQUEST,
					"dashboard.saveFailForAtLeastOneTemplate");

		User user = WebUtils.getUser(request, response);

		HtmlTplDashboardWidgetEntity widget = this.htmlTplDashboardWidgetEntityService.getById(user, id);

		if (widget == null)
			throw new RecordNotFoundException();

		trimResourceNames(templates);
		widget.setTemplates(templates);

		this.htmlTplDashboardWidgetEntityService.update(user, widget);

		Map<String, Object> data = new HashMap<>();
		data.put("id", id);
		data.put("templates", templates);

		ResponseEntity<OperationMessage> responseEntity = buildOperationMessageSaveSuccessResponseEntity(request);
		responseEntity.getBody().setData(data);

		return responseEntity;
	}

	@RequestMapping(value = "/getTemplateContent", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public Map<String, Object> getTemplateContent(HttpServletRequest request, HttpServletResponse response,
			org.springframework.ui.Model model, @RequestParam("id") String id,
			@RequestParam("templateName") String templateName) throws Exception
	{
		User user = WebUtils.getUser(request, response);

		HtmlTplDashboardWidgetEntity widget = this.htmlTplDashboardWidgetEntityService.getById(user, id);

		if (widget == null)
			throw new RecordNotFoundException();

		Map<String, Object> data = new HashMap<>();
		data.put("id", id);
		data.put("templateName", templateName);
		data.put("templateContent", readTemplateContent(widget, templateName));

		return data;
	}

	@RequestMapping(value = "/listResources", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public List<String> listResources(HttpServletRequest request, HttpServletResponse response,
			org.springframework.ui.Model model, @RequestParam("id") String id) throws Exception
	{
		User user = WebUtils.getUser(request, response);

		HtmlTplDashboardWidgetEntity dashboard = this.htmlTplDashboardWidgetEntityService.getById(user, id);

		if (dashboard == null)
			return new ArrayList<>(0);

		TemplateDashboardWidgetResManager dashboardWidgetResManager = this.htmlTplDashboardWidgetEntityService
				.getHtmlTplDashboardWidgetRenderer().getTemplateDashboardWidgetResManager();

		List<String> resources = dashboardWidgetResManager.listResources(dashboard.getId());

		return resources;
	}

	@RequestMapping(value = "/deleteResource", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> deleteResource(HttpServletRequest request, HttpServletResponse response,
			org.springframework.ui.Model model, @RequestParam("id") String id, @RequestParam("name") String name)
			throws Exception
	{
		name = trimResourceName(name);

		User user = WebUtils.getUser(request, response);

		HtmlTplDashboardWidgetEntity dashboard = this.htmlTplDashboardWidgetEntityService.getByIdForEdit(user, id);

		if (dashboard == null)
			throw new RecordNotFoundException();

		TemplateDashboardWidgetResManager dashboardWidgetResManager = this.htmlTplDashboardWidgetEntityService
				.getHtmlTplDashboardWidgetRenderer().getTemplateDashboardWidgetResManager();

		dashboardWidgetResManager.delete(id, name);

		return buildOperationMessageSuccessEmptyResponseEntity();
	}

	@RequestMapping(value = "/uploadResourceFile", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public Map<String, Object> uploadResourceFile(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("file") MultipartFile multipartFile) throws Exception
	{
		File tmpDirectory = FileUtil.generateUniqueDirectory(this.tempDirectory);
		String fileName = multipartFile.getOriginalFilename();
		File file = FileUtil.getFile(tmpDirectory, fileName);

		InputStream in = null;
		OutputStream out = null;
		try
		{
			in = multipartFile.getInputStream();
			out = IOUtil.getOutputStream(file);
			IOUtil.write(in, out);
		}
		finally
		{
			IOUtil.close(in);
			IOUtil.close(out);
		}

		String uploadFilePath = FileUtil.getRelativePath(this.tempDirectory, file);

		Map<String, Object> results = new HashMap<>();
		results.put("uploadFilePath", uploadFilePath);
		results.put("fileName", fileName);

		return results;
	}

	@RequestMapping(value = "/saveResourceFile", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveResourceFile(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("id") String id, @RequestParam("resourceFilePath") String resourceFilePath,
			@RequestParam("resourceName") String resourceName) throws Exception
	{
		User user = WebUtils.getUser(request, response);

		HtmlTplDashboardWidgetEntity dashboard = this.htmlTplDashboardWidgetEntityService.getByIdForEdit(user, id);

		if (dashboard == null)
			throw new RecordNotFoundException();

		File uploadFile = FileUtil.getDirectory(this.tempDirectory, resourceFilePath, false);

		if (!uploadFile.exists())
			throw new IllegalInputException();

		TemplateDashboardWidgetResManager dashboardWidgetResManager = this.htmlTplDashboardWidgetEntityService
				.getHtmlTplDashboardWidgetRenderer().getTemplateDashboardWidgetResManager();

		InputStream in = null;
		OutputStream out = null;

		try
		{
			in = IOUtil.getInputStream(uploadFile);
			out = dashboardWidgetResManager.getResourceOutputStream(id, resourceName);

			IOUtil.write(in, out);
		}
		finally
		{
			IOUtil.close(in);
			IOUtil.close(out);
		}

		return buildOperationMessageSaveSuccessResponseEntity(request);
	}

	@RequestMapping("/import")
	public String impt(HttpServletRequest request, org.springframework.ui.Model model)
	{
		return "/analysis/dashboard/dashboard_import";
	}

	@RequestMapping(value = "/uploadImportFile", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public Map<String, Object> uploadImportFile(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("file") MultipartFile multipartFile) throws Exception
	{
		String dasboardName = "";
		String dashboardFileName = "";
		List<String> templates = new ArrayList<>();

		File tmpDirectory = FileUtil.generateUniqueDirectory(this.tempDirectory);

		dashboardFileName = tmpDirectory.getName();

		String fileName = multipartFile.getOriginalFilename();

		if (FileUtil.isExtension(fileName, "zip"))
		{
			ZipInputStream in = IOUtil.getZipInputStream(multipartFile.getInputStream());
			try
			{
				IOUtil.unzip(in, tmpDirectory);
			}
			finally
			{
				IOUtil.close(in);
			}

			File[] files = tmpDirectory.listFiles();
			if (files != null)
			{
				for (File file : files)
				{
					if (file.isDirectory())
						continue;

					String name = file.getName();

					if (name.equalsIgnoreCase("index.html") || name.equalsIgnoreCase("index.htm"))
						templates.add(0, name);
					else if (FileUtil.isExtension(name, "html") || FileUtil.isExtension(name, "htm"))
						templates.add(name);
				}
			}
		}
		else
		{
			File file = FileUtil.getFile(tmpDirectory, fileName);

			InputStream in = null;
			OutputStream out = null;
			try
			{
				in = multipartFile.getInputStream();
				out = IOUtil.getOutputStream(file);
				IOUtil.write(in, out);
			}
			finally
			{
				IOUtil.close(in);
				IOUtil.close(out);
			}

			templates.add(fileName);
		}

		dasboardName = FileUtil.deleteExtension(fileName);

		Map<String, Object> results = new HashMap<>();

		results.put("dashboardName", dasboardName);
		results.put("template", HtmlTplDashboardWidgetEntity.concatTemplates(templates));
		results.put("dashboardFileName", dashboardFileName);

		return results;
	}

	@RequestMapping(value = "/saveImport", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveImport(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("name") String name, @RequestParam("template") String template,
			@RequestParam("dashboardFileName") String dashboardFileName) throws Exception
	{
		File uploadDirectory = FileUtil.getDirectory(this.tempDirectory, dashboardFileName, false);

		if (!uploadDirectory.exists())
			throw new IllegalInputException();

		String[] templates = HtmlTplDashboardWidgetEntity.splitTemplates(template);

		if (isEmpty(templates))
			throw new IllegalInputException();

		for (String fileName : templates)
		{
			File templateFile = FileUtil.getFile(uploadDirectory, fileName);

			if (!templateFile.exists() || templateFile.isDirectory())
				return buildOperationMessageFailResponseEntity(request, HttpStatus.BAD_REQUEST,
						"dashboard.import.templateFileNotExists", fileName);
		}

		String templateEncoding = resolveTemplateEncoding(FileUtil.getFile(uploadDirectory, templates[0]));

		User user = WebUtils.getUser(request, response);

		HtmlTplDashboardWidgetEntity dashboard = new HtmlTplDashboardWidgetEntity();
		dashboard.setTemplateSplit(template);
		dashboard.setTemplateEncoding(templateEncoding);
		dashboard.setName(name);

		checkSaveEntity(dashboard);

		dashboard.setId(IDUtil.randomIdOnTime20());
		dashboard.setCreateUser(user);

		this.htmlTplDashboardWidgetEntityService.add(user, dashboard);

		TemplateDashboardWidgetResManager dashboardWidgetResManager = this.htmlTplDashboardWidgetEntityService
				.getHtmlTplDashboardWidgetRenderer().getTemplateDashboardWidgetResManager();

		dashboardWidgetResManager.copyFrom(dashboard.getId(), uploadDirectory);

		return buildOperationMessageSaveSuccessResponseEntity(request);
	}

	@RequestMapping("/view")
	public String view(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model,
			@RequestParam("id") String id) throws Exception
	{
		User user = WebUtils.getUser(request, response);

		HtmlTplDashboardWidgetEntity dashboard = this.htmlTplDashboardWidgetEntityService.getById(user, id);

		if (dashboard == null)
			throw new RecordNotFoundException();

		model.addAttribute("dashboard", dashboard);
		model.addAttribute("templates", toWriteJsonTemplateModel(dashboard.getTemplates()));
		model.addAttribute("templateName", dashboard.getFirstTemplate());
		model.addAttribute("templateContent", readTemplateContent(dashboard, dashboard.getFirstTemplate()));
		model.addAttribute(KEY_TITLE_MESSAGE_KEY, "dashboard.viewDashboard");
		model.addAttribute(KEY_READONLY, true);

		return "/analysis/dashboard/dashboard_form";
	}

	@RequestMapping(value = "/delete", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> delete(HttpServletRequest request, HttpServletResponse response,
			@RequestBody String[] ids)
	{
		User user = WebUtils.getUser(request, response);

		for (int i = 0; i < ids.length; i++)
		{
			String id = ids[i];
			this.htmlTplDashboardWidgetEntityService.deleteById(user, id);
		}

		return buildOperationMessageDeleteSuccessResponseEntity(request);
	}

	@RequestMapping("/pagingQuery")
	public String pagingQuery(HttpServletRequest request, HttpServletResponse response,
			org.springframework.ui.Model model)
	{
		User user = WebUtils.getUser(request, response);
		model.addAttribute("currentUser", user);

		model.addAttribute(KEY_TITLE_MESSAGE_KEY, "dashboard.manageDashboard");

		return "/analysis/dashboard/dashboard_grid";
	}

	@RequestMapping(value = "/select")
	public String select(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model)
	{
		model.addAttribute(KEY_TITLE_MESSAGE_KEY, "dashboard.selectDashboard");
		model.addAttribute(KEY_SELECT_OPERATION, true);

		return "/analysis/dashboard/dashboard_grid";
	}

	@RequestMapping(value = "/pagingQueryData", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public PagingData<HtmlTplDashboardWidgetEntity> pagingQueryData(HttpServletRequest request,
			HttpServletResponse response, final org.springframework.ui.Model springModel,
			@RequestBody(required = false) DataFilterPagingQuery pagingQueryParam) throws Exception
	{
		User user = WebUtils.getUser(request, response);
		final DataFilterPagingQuery pagingQuery = inflateDataFilterPagingQuery(request, pagingQueryParam);

		PagingData<HtmlTplDashboardWidgetEntity> pagingData = this.htmlTplDashboardWidgetEntityService.pagingQuery(user,
				pagingQuery, pagingQuery.getDataFilter());

		return pagingData;
	}

	/**
	 * 展示看板首页。
	 * 
	 * @param request
	 * @param response
	 * @param model
	 * @param id
	 * @throws Exception
	 */
	@RequestMapping("/show/{id}/")
	public void show(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model,
			@PathVariable("id") String id) throws Exception
	{
		User user = WebUtils.getUser(request, response);
		HtmlTplDashboardWidgetEntity dashboardWidget = this.htmlTplDashboardWidgetEntityService
				.getHtmlTplDashboardWidget(user, id);

		showDashboard(request, response, model, user, dashboardWidget, dashboardWidget.getFirstTemplate());
	}

	/**
	 * 加载看板资源。
	 * 
	 * @param request
	 * @param response
	 * @param webRequest
	 * @param model
	 * @param id
	 * @throws Exception
	 */
	@RequestMapping("/show/{id}/**/*")
	public void showResource(HttpServletRequest request, HttpServletResponse response, WebRequest webRequest,
			org.springframework.ui.Model model, @PathVariable("id") String id) throws Exception
	{
		User user = WebUtils.getUser(request, response);
		HtmlTplDashboardWidgetEntity entity = this.htmlTplDashboardWidgetEntityService.getById(user, id);

		if (entity == null)
			throw new RecordNotFoundException();

		String resName = resolvePathAfter(request, "/show/" + id + "/");

		if (entity.isTemplate(resName))
		{
			HtmlTplDashboardWidgetEntity dashboardWidget = this.htmlTplDashboardWidgetEntityService
					.getHtmlTplDashboardWidget(user, id);

			showDashboard(request, response, model, user, dashboardWidget, resName);
		}
		else
		{
			TemplateDashboardWidgetResManager resManager = this.htmlTplDashboardWidgetEntityService
					.getHtmlTplDashboardWidgetRenderer().getTemplateDashboardWidgetResManager();

			if (!resManager.containsResource(id, resName))
				throw new FileNotFoundException(resName);

			setContentTypeByName(request, response, getServletContext(), resName);

			long lastModified = resManager.lastModifiedResource(id, resName);
			if (webRequest.checkNotModified(lastModified))
				return;

			InputStream in = resManager.getResourceInputStream(id, resName);
			OutputStream out = response.getOutputStream();

			try
			{
				IOUtil.write(in, out);
			}
			finally
			{
				IOUtil.close(in);
			}
		}
	}

	/**
	 * 展示看板。
	 * 
	 * @param request
	 * @param response
	 * @param model
	 * @param id
	 * @throws Exception
	 */
	protected void showDashboard(HttpServletRequest request, HttpServletResponse response,
			org.springframework.ui.Model model, User user, HtmlTplDashboardWidgetEntity dashboardWidget,
			String template) throws Exception
	{
		if (dashboardWidget == null)
			throw new RecordNotFoundException();

		// 确保看板创建用户对看板模板内定义的图表有权限
		ChartWidgetSourceContext.set(new ChartWidgetSourceContext(dashboardWidget.getCreateUser()));

		try
		{
			TemplateDashboardWidgetResManager dashboardWidgetResManager = this.htmlTplDashboardWidgetEntityService
					.getHtmlTplDashboardWidgetRenderer().getTemplateDashboardWidgetResManager();

			String responseEncoding = dashboardWidget.getTemplateEncoding();

			if (StringUtil.isEmpty(responseEncoding))
				responseEncoding = dashboardWidgetResManager.getDefaultEncoding();

			response.setCharacterEncoding(responseEncoding);
			response.setContentType(CONTENT_TYPE_HTML);

			Writer out = response.getWriter();

			RenderStyle renderStyle = resolveRenderStyle(request);
			HtmlRenderContext renderContext = createHtmlRenderContext(request, createWebContext(request), renderStyle,
					out);
			DashboardTheme dashboardTheme = getHtmlTplDashboardWidgetEntityService().getHtmlTplDashboardWidgetRenderer()
					.inflateDashboardTheme(renderContext, renderStyle);
			AddPrefixHtmlTitleHandler htmlTitleHandler = new AddPrefixHtmlTitleHandler(
					getMessage(request, "dashboard.show.htmlTitlePrefix", getMessage(request, "app.name")));
			HtmlRenderAttributes.setHtmlTitleHandler(renderContext, htmlTitleHandler);
			setDashboardThemeAttribute(request.getSession(), dashboardTheme);

			HtmlTplDashboard dashboard = dashboardWidget.render(renderContext, template);

			SessionHtmlTplDashboardManager dashboardManager = getSessionHtmlTplDashboardManagerNotNull(request);
			dashboardManager.put(dashboard);
		}
		finally
		{
			ChartWidgetSourceContext.remove();
		}
	}

	/**
	 * 看板样式。
	 * <p>
	 * 根据当前渲染风格动态生成看板样式。
	 * </p>
	 * 
	 * @param request
	 * @param response
	 * @param model
	 * @throws Exception
	 */
	@RequestMapping("/showStyle")
	public void showStyle(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model)
			throws Exception
	{
		// 不缓存
		response.setDateHeader("Expires", -1);
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Pragma", "no-cache");
		response.setContentType(CONTENT_TYPE_CSS);
		PrintWriter out = response.getWriter();

		DashboardTheme dashboardTheme = getDashboardThemeAttribute(request.getSession());
		ChartTheme chartTheme = (dashboardTheme == null ? null : dashboardTheme.getChartTheme());

		if (chartTheme != null)
		{
			// 表格行
			out.println(".dg-chart-table .dg-chart-table-content table.dataTable tbody tr{");
			out.println("	background:" + chartTheme.getBackgroundColor() + ";");
			out.println("}");

			// 表格奇数行
			out.println(".dg-chart-table .dg-chart-table-content table.dataTable.stripe tbody tr.odd,"
					+ " .dg-chart-table .dg-chart-table-content table.dataTable.display tbody tr.odd{");
			out.println("	background:" + chartTheme.getBorderColor() + ";");
			out.println("}");

			// 表格选中、悬浮，拷贝自/src/main/resources/org/datagear/web/webapp/static/theme/lightness/common.css
			out.println(".dg-chart-table .dg-chart-table-content table.dataTable.hover tbody tr.hover,");
			out.println(".dg-chart-table .dg-chart-table-content table.dataTable.hover tbody tr:hover,");
			out.println(".dg-chart-table .dg-chart-table-content table.dataTable.display tbody tr:hover {");
			out.println("	background-color: " + chartTheme.getAxisScaleLineColor() + ";");
			out.println("}");

			out.println(".dg-chart-table .dg-chart-table-content table.dataTable.hover tbody tr.hover.selected,");
			out.println(".dg-chart-table .dg-chart-table-content table.dataTable tbody > tr.selected,");
			out.println(".dg-chart-table .dg-chart-table-content table.dataTable tbody > tr > .selected,");
			out.println(".dg-chart-table .dg-chart-table-content table.dataTable.stripe tbody > tr.odd.selected,");
			out.println(
					".dg-chart-table .dg-chart-table-content table.dataTable.stripe tbody > tr.odd > .selected,");
			out.println(".dg-chart-table .dg-chart-table-content table.dataTable.display tbody > tr.odd.selected,");
			out.println(
					".dg-chart-table .dg-chart-table-content table.dataTable.display tbody > tr.odd > .selected,");
			out.println(".dg-chart-table .dg-chart-table-content table.dataTable.hover tbody > tr.selected:hover,");
			out.println(
					".dg-chart-table .dg-chart-table-content table.dataTable.hover tbody > tr > .selected:hover,");
			out.println(
					".dg-chart-table .dg-chart-table-content table.dataTable.display tbody > tr.selected:hover,");
			out.println(
					".dg-chart-table .dg-chart-table-content table.dataTable.display tbody > tr > .selected:hover {");
			out.println("	background-color: " + chartTheme.getHighlightTheme().getBackgroundColor() + ";");
			out.println("	color: " + chartTheme.getHighlightTheme().getColor() + ";");
			out.println("}");
		}
	}

	/**
	 * 看板数据。
	 * 
	 * @param request
	 * @param response
	 * @param model
	 * @param id
	 * @throws Exception
	 */
	@RequestMapping(value = "/showData", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public Map<String, DataSetResult[]> showData(HttpServletRequest request, HttpServletResponse response,
			org.springframework.ui.Model model, @RequestBody Map<String, ?> paramData) throws Exception
	{
		WebContext webContext = createWebContext(request);
		return getDashboardData(request, response, model, webContext, paramData);
	}

	/**
	 * 动态加载看板图表。
	 * 
	 * @param request
	 * @param response
	 * @param model
	 * @param dashbaordId
	 * @throws Throwable
	 */
	@RequestMapping(value = "/loadChart", produces = CONTENT_TYPE_JSON)
	public void loadChart(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model,
			@RequestParam("dashboardId") String dashboardId, @RequestParam("chartWidgetId") String chartWidgetId,
			@RequestParam("chartElementId") String chartElementId)
			throws Throwable
	{
		SessionHtmlTplDashboardManager dashboardManager = getSessionHtmlTplDashboardManagerNotNull(request);
		HtmlTplDashboard dashboard = dashboardManager.get(dashboardId);

		if (dashboard == null)
			throw new RecordNotFoundException();

		HtmlTplDashboardWidgetEntity dashboardWidget = (HtmlTplDashboardWidgetEntity) dashboard.getWidget();

		// 确保看板创建用户对看板模板内定义的图表有权限
		ChartWidgetSourceContext.set(new ChartWidgetSourceContext(dashboardWidget.getCreateUser()));

		ChartWidgetSource chartWidgetSource = getHtmlTplDashboardWidgetEntityService()
				.getHtmlTplDashboardWidgetRenderer().getChartWidgetSource();

		ChartWidget<HtmlRenderContext> chartWidget = chartWidgetSource.getChartWidget(chartWidgetId);

		if (chartWidget == null)
			throw new RecordNotFoundException();

		// 不缓存
		response.setContentType(CONTENT_TYPE_JSON);
		PrintWriter out = response.getWriter();

		RenderStyle renderStyle = resolveRenderStyle(request);
		HtmlRenderContext renderContext = createHtmlRenderContext(request, createWebContext(request), renderStyle,
				out);

		HtmlChartPluginRenderOption renderOption = new HtmlChartPluginRenderOption();
		renderOption.setChartElementId(chartElementId);
		renderOption.setNotWriteChartElement(true);
		renderOption.setPluginVarName(
				"{\"id\": \"" + WebUtils.escapeJavaScriptStringValue(chartWidget.getPlugin().getId()) + "\"}");
		renderOption.setNotWritePluginObject(true);
		renderOption.setChartVarName("undefined");
		renderOption.setRenderContextVarName("{}");
		renderOption.setNotWriteRenderContextObject(true);
		renderOption.setNotWriteScriptTag(true);
		renderOption.setNotWriteInvoke(true);
		renderOption.setWriteChartJson(true);
		HtmlChartPluginRenderOption.setOption(renderContext, renderOption);

		Chart chart = chartWidget.render(renderContext);

		synchronized (dashboard)
		{
			List<Chart> charts = dashboard.getCharts();
			List<Chart> newCharts = (charts == null || charts.isEmpty() ? new ArrayList<Chart>()
					: new ArrayList<Chart>(charts));
			newCharts.add(chart);
			dashboard.setCharts(newCharts);
		}
	}

	/**
	 * 解析HTML模板的字符编码。
	 * 
	 * @param templateIn
	 * @return
	 * @throws IOException
	 */
	protected String resolveTemplateEncoding(String templateContent) throws IOException
	{
		String templateEncoding = null;

		if (templateContent != null)
		{
			Reader in = null;
			try
			{
				in = IOUtil.getReader(templateContent);

				templateEncoding = this.htmlTplDashboardWidgetEntityService.getHtmlTplDashboardWidgetRenderer()
						.resolveCharset(in);
			}
			finally
			{
				IOUtil.close(in);
			}
		}

		if (StringUtil.isEmpty(templateEncoding))
			templateEncoding = HtmlTplDashboardWidget.DEFAULT_TEMPLATE_ENCODING;

		return templateEncoding;
	}

	/**
	 * 解析HTML模板文件的字符编码。
	 * 
	 * @param templateIn
	 * @return
	 * @throws IOException
	 */
	protected String resolveTemplateEncoding(File templateFile) throws IOException
	{
		String templateEncoding = null;

		InputStream in = null;
		try
		{
			in = IOUtil.getInputStream(templateFile);

			templateEncoding = this.htmlTplDashboardWidgetEntityService.getHtmlTplDashboardWidgetRenderer()
					.resolveCharset(in);
		}
		finally
		{
			IOUtil.close(in);
		}

		if (StringUtil.isEmpty(templateEncoding))
			templateEncoding = HtmlTplDashboardWidget.DEFAULT_TEMPLATE_ENCODING;

		return templateEncoding;
	}

	protected WebContext createWebContext(HttpServletRequest request)
	{
		String contextPath = getWebContextPath(request).get(request);
		return new WebContext(contextPath, contextPath + "/analysis/dashboard/showData");
	}

	protected void checkSaveEntity(HtmlTplDashboardWidgetEntity widget)
	{
		if (isBlank(widget.getName()))
			throw new IllegalInputException();

		if (isEmpty(widget.getTemplates()))
			throw new IllegalInputException();
	}

	protected String readTemplateContent(HtmlTplDashboardWidgetEntity widget, String templateName) throws IOException
	{
		String templateContent = this.htmlTplDashboardWidgetEntityService.getHtmlTplDashboardWidgetRenderer()
				.readTemplateContent(widget, templateName);

		return templateContent;
	}

	protected void saveTemplateContent(HtmlTplDashboardWidgetEntity widget, String templateName, String templateContent)
			throws IOException
	{
		this.htmlTplDashboardWidgetEntityService.getHtmlTplDashboardWidgetRenderer().saveTemplateContent(widget,
				templateName, templateContent);
	}

	protected void trimResourceNames(String[] resourceNames)
	{
		if (resourceNames == null)
			return;

		for (int i = 0; i < resourceNames.length; i++)
			resourceNames[i] = trimResourceName(resourceNames[i]);
	}

	protected String trimResourceName(String resourceName)
	{
		if (StringUtil.isEmpty(resourceName))
			return "";

		resourceName = FileUtil.trimPath(resourceName, FileUtil.PATH_SEPARATOR_SLASH);
		if (resourceName.startsWith(FileUtil.PATH_SEPARATOR_SLASH))
			resourceName = resourceName.substring(1);

		return resourceName;
	}
}
