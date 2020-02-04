package com.app.mdc.utils.file;

import org.jodconverter.OfficeDocumentConverter;
import org.jodconverter.office.DefaultOfficeManagerBuilder;
import org.jodconverter.office.OfficeException;
import org.jodconverter.office.OfficeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class FileUtil {

	private static Logger logger = LoggerFactory.getLogger(FileUtil.class);

	private static OfficeManager officeManager=null;
	/**
	 * 将MultipartFile保存到指定的路径下
	 * 
	 * @param file
	 *            Spring的MultipartFile对象
	 * @param savePath
	 *            保存路径
	 * @return 保存的文件名，当返回NULL时为保存失败。
	 * @throws IOException ioException
	 * @throws IllegalStateException IllegalStateException
	 */
	public static String save(MultipartFile file, String savePath) throws IllegalStateException, IOException {
		if (file != null && file.getSize() > 0) {
			File fileFolder = new File(savePath);
			if (!fileFolder.exists()) {
				fileFolder.mkdirs();
			}
			File saveFile = getFile(savePath, file.getOriginalFilename());
			file.transferTo(saveFile);
			return saveFile.getName();
		}
		return null;
	}

	private static File getFile(String savePath, String originalFilename) {
		String fileName = FileUtil.getUUIDFileName(originalFilename);
		File file = new File(savePath + fileName);
		if (file.exists()) {
			return getFile(savePath, originalFilename);
		}
		return file;
	}

	/**
	 * 删除文件
	 * 
	 * @param filePath
	 *            文件路径
	 * @return 是否删除成功：true-删除成功，false-删除失败
	 */
	public static boolean delete(String filePath) {
		File file = new File(filePath);
		if (file.isFile()) {
			file.delete();
			return true;
		}
		return false;
	}

	/**
	 * 生成唯一的文件名:
	 */
	public static String getUUIDFileName(String fileName){
		// 将文件名的前面部分进行截取：xx.jpg   --->   .jpg
		int idx = fileName.lastIndexOf(".");
		String extention = fileName.substring(idx);
		return UUID.randomUUID().toString().replace("-", "")+extention;
	}

	public static String getDateFileName(String fileName){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSS");
		String resTime = sdf.format(new Date());
		return resTime+"_"+fileName;
	}

	public static boolean isNotEmpty(MultipartFile multipartFile){
		return multipartFile != null && multipartFile.getSize() > 0;
	}

	/**
	 * 根据路径判断文件是否存在
	 * @param filePath 文件路径
	 * @return boolean
	 */
	public static boolean isExist(String filePath){
		File file = new File(filePath);
		return file.exists();
	}

	/**
	 * 开启OfficeManager服务
	 * @return OfficeManager
	 */
	public static OfficeManager processStart(String libreOfficePath){
		//String libreOfficePath = picConfig.getLibreOffice();
		logger.info("准备开启officeManager服务!");
		try {
			// 此类在jodconverter-core中3版本中存在，在2.2.2版本中不存在
			DefaultOfficeManagerBuilder configuration = new DefaultOfficeManagerBuilder();
			// libreOffice的安装目录
			logger.info("设置libreOffice安装目录:"+libreOfficePath);
			configuration.setOfficeHome(new File(libreOfficePath));
			// 设置端口号
			logger.info("设置服务端口:"+8100);
			configuration.setPortNumbers(8100);
			// 设置任务执行超时为5分钟
			logger.info("设置任务执行超时时间:5分钟");
			configuration.setTaskExecutionTimeout(1000 * 60 * 5L);
			// 设置任务队列超时为24小时
			logger.info("设置任务队列超时时间:24小时");
			configuration.setTaskQueueTimeout(1000 * 60 * 60 * 24L);
			// 开启转换服务
			logger.info("开启转换服务");
			officeManager = configuration.build();
			officeManager.start();
			logger.info("服务开启成功!");
		}catch (OfficeException e){
			logger.error("officeManager启动异常",e);
		}
		return officeManager;
	}

	/**
	 * 开始文件转换
	 * @param inputFile 需要转换的文件
	 * @param fileChangePath 转换后文件保存地址
	 * @param libreOffice libreOffice程序目录
	 * @return 转换后文件地址
	 */
	public static String libreOfficeAndJodconverter(String inputFile,String fileChangePath,String libreOffice){
		logger.info("开始将文件"+inputFile+"转换为pdf文件");
		String outputFile = "";
		try {
			if(officeManager == null || !officeManager.isRunning()){
				officeManager = FileUtil.processStart(libreOffice);
			}
			logger.info("获取需要转换的文件:"+inputFile);
			File src = new File(inputFile);
			if(src.exists()){
				File changeFile = new File(fileChangePath);
				if(!changeFile.exists()){
					changeFile.mkdir();
				}
				outputFile = fileChangePath+UUID.randomUUID().toString().replace("-","")+".pdf";
				logger.info("设置转换后的文件:"+outputFile);
				File dst = new File(outputFile);
				OfficeDocumentConverter converter = new OfficeDocumentConverter(officeManager);
				logger.info("转换文件"+inputFile+"为"+outputFile);
				converter.convert(src,dst);
			}else{
				logger.error("需要转换的文件不存在，请重新选择需要转换的文件!");
			}
			return outputFile;
		} catch (OfficeException e){
			logger.error("文件转换失败",e);
			return outputFile;
		}
		finally {
//			FileUtil fileUtil = new FileUtil();
//			fileUtil.processStop(outputFile,officeManager);
		}
		//return outputFile;
	}


	/**
	 * 停止office服务
	 * @param path 文件路径
	 * @param officeManager officeManager
	 */
	@Async
	public void processStop(String path,OfficeManager officeManager){
		if (officeManager != null && !"".equals(path)){
			try {
				logger.info("关闭officeManager服务!");
				officeManager.stop();
			}catch (OfficeException oc){
				logger.error("服务关闭失败",oc);
			}
		}
	}

	public static String xDocServiceChangeFile(String inputFile,String fileChangePath){
		XDocService xDocService = new XDocService();
		String outputFile = "";
		File src = new File(inputFile);
		if(src.exists()){
			File changeFile = new File(fileChangePath);
			if(!changeFile.exists()){
				changeFile.mkdir();
			}
			outputFile = fileChangePath+UUID.randomUUID().toString().replace("-","")+".pdf";
			logger.info("设置转换后的文件:"+outputFile);
			try {
				xDocService.to(new java.io.File(inputFile),new java.io.File(outputFile));
			}catch (IOException e){
				e.printStackTrace();
			}
			logger.info("转换文件"+inputFile+"为"+outputFile);
		}else{
			logger.error("需要转换的文件不存在，请重新选择需要转换的文件!");
		}
		return outputFile;
	}

	public static void main(String[] args) {
		System.out.println(UUID.randomUUID().toString().replace("-", ""));
	}
}