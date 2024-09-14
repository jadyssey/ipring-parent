* [公司新来一个技术总监，把支付系统设计得炉火纯青，那叫一个优雅，佩服！ (qq.com)](https://mp.weixin.qq.com/s/QXvlvt02D9kluuWknqbIjw)
* [智牛股: 智牛股金融交易平台是服务于金融衍生产品， 包含外汇、贵金属、期货、股票等。 各产品具有不同属性与交易规则， 本项目对标MT4/MT5等大型交易平台， 遵循FIX全球最广泛的金融市场通用协议。 包含证券用户注册、交易开户、行情订阅与呈现、 市价下单、委托挂单、撤单、 撮合交易等核心业务功能。 (gitee.com)](https://gitee.com/itxinfei/bulls-stock)

在支付场景中，不但涉及诸多的复杂业务，结算规则，超长的流程，第三方对接，其中更是涉及到诸多技术细节，比如：**事务管理、异步处理、重试机制、加锁**等；下面来分析具体的细节逻辑。

![Image](https://mmbiz.qpic.cn/mmbiz_png/JdLkEI9sZfelVklLaMTOW4tlo2rkdqZIOI9YTjQRRQctpqibNVncQ4aS1jWSWKzpjpsOs89RA8KP7VMiaMkeXodw/640?wx_fmt=png&from=appmsg&wxfrom=5&wx_lazy=1&wx_co=1&tp=webp)

如图是对交易场景常见的分解，大致可以分为四个模块：

- **账面管理**：对于开通支付功能的用户，必须清晰的管理资金信息；比如可用，冻结，账单等；
- **交易流水**：整个资金管理的流水记录，不局限于交易场景，还有充值，提现，退款等；
- **支付对接**：通常流程中的支付功能都是对接第三方支付平台来实现的，所以要做好请求和报文的记录；
- **订单结构**：比如在电商交易中，订单模型的管理，拆单策略等，支付的商品规格等；



**流程时序**

![Image](https://mmbiz.qpic.cn/mmbiz_png/JdLkEI9sZfelVklLaMTOW4tlo2rkdqZIYSWYRibyKOX3YeibvYG3lqnPmZcz47HPBjvu5nicoY1ibb1OQa7NnL9MOQ/640?wx_fmt=png&from=appmsg&wxfrom=5&wx_lazy=1&wx_co=1&tp=webp)



**支付系统**

支付分三层，订单、核心、渠道，系统这三层关系如何设计的，重复支付怎么判断，改单支付如何判断，账务如何设计的

支付系统不是普通的低含金量的业务系统，包含了完整的商品、会员、订单等多个模块，以及支付宝沙箱、支付宝PC接入、支付回调、账户充值、退款、支付查询、支付对账、微信支付接入、返现与提现、统一支付、支付报表

**支付场景**：线下支付、线上支付



RocketMQ消息队列实现支付结果异步通知



