package org.nlpcn.jcoder.util;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.nlpcn.commons.lang.util.FileFinder;
import org.nlpcn.commons.lang.util.IOUtil;
import org.nlpcn.commons.lang.util.ObjConver;
import org.nlpcn.commons.lang.util.StringUtil;
import org.nlpcn.jcoder.server.rpc.client.RpcClient;
import org.nlpcn.jcoder.server.rpc.client.RpcRequest;
import org.nutz.ioc.Ioc;
import org.nutz.ioc.impl.NutIoc;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.json.JsonLoader;
import org.nutz.lang.Mirror;

/**
 * Test your task
 * 
 * @author Ansj
 *
 */
public class Testing {

	public static final String CODE_RUN = "123__CODE__RUN";

	/**
	 * instan task by ioc
	 * 
	 * @param c
	 * @return class c instance
	 * @throws Exception
	 */
	public static <T> T instance(Class<T> c, String iocPath) throws Exception {
		Ioc ioc = new NutIoc(new JsonLoader(iocPath));

		Mirror<?> mirror = Mirror.me(c);
		T obj = c.newInstance();

		for (Field field : mirror.getFields()) {
			Inject inject = field.getAnnotation(Inject.class);
			if (inject != null) {
				if (field.getType().equals(Logger.class)) {
					mirror.setValue(obj, field, Logger.getLogger(c));
				} else {
					mirror.setValue(obj, field, ioc.get(field.getType(), StringUtil.isBlank(inject.value()) ? field.getName() : inject.value()));
				}
			}
		}

		return obj;
	}

	public static <T> T instance(Class<T> c) throws Exception {
		return instance(c, "resource/ioc.js");
	}

	/**
	 * 提交本地代码到远程运行,
	 * 
	 * @param timeout 超时时间,毫秒
	 * @param c 本地类文件
	 * @param methodName 运行的方法名称
	 * @param params 方法传入的参数,
	 * @return object 返回结果
	 * @throws Throwable
	 */
	public static Object remote(int timeout, Class<?> c, String methodName, Object... params) throws Throwable {
		// 根据class 获得代码

		File codeFile = FileFinder.find(c.getName().replace(".", System.getProperty("file.separator")) + ".java");

		String code = IOUtil.getContent(codeFile, "utf-8");

		ResourceBundle rb = ResourceBundle.getBundle("remote_code_config");

		String host = rb.getString("host");

		int port = ObjConver.getIntValue(rb.getString("port"));

		HashMap<String, String> map = new HashMap<>();
		map.put("code", code);
		map.put("name", rb.getString("name"));
		map.put("password", StaticValue.passwordEncoding(rb.getString("password")));
		
		try {
			RpcClient.connect(host, port);
			RpcRequest req = new RpcRequest();
			req.setClassName(CODE_RUN);
			req.setMessageId(UUID.randomUUID().toString());
			req.setMethodName(methodName);
			params = Arrays.copyOf(params, params.length + 1);
			params[params.length - 1] = map;
			req.setArguments(params);
			return RpcClient.getInstance().proxy(req);
		} finally {
			RpcClient.shutdown();
		}

	}
	
}
