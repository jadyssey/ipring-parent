package org.ipring.client.baseservice;

import lombok.Data;

/**
 * <p>
 * SendEmailSubmit
 * </p>
 *
 * @author Crab.B
 * @date 2023/1/6 17:45
 */
@Data
public class SendEmailSubmit {

	/**
	 * 接收邮箱，群发用英文逗号隔开，如：123@qq.com,456@163.com
	 */
	private String email;

	/**
	 * 自定义主题，如果需要显示验证码，用${code}代替
	 */
	private String customSubject;

	/**
	 * 自定义内容，验证码用${code}代替
	 */
	private String customContent;

	/**
	 * 产品ID
	 */
	private Integer productType;

	/**
	 * 发送类型(注册 = 1,修改密码 = 2,更换手机号码 = 3,更换邮箱 = 4,绑定邮箱 = 5,绑定手机 = 6,验证短信 = 7)
	 */
	private Integer sendType;

	/**
	 * 群发单显，1：是，0：否（默认为否，群发邮件时传）
	 */
	private Integer singleShow = 1;

	/**
	 * 设备号
	 */
	private String deviceIp;

	/**
	 * 请求ip
	 */
	private String requestIp;

	/**
	 * smtp服务类型
	 */
	private Integer smtpType;
}
