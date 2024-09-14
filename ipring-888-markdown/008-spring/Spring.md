Spring6：2 入门 - Lixx Blog - 李晓旭的博客](https://lixx.cn/archives/spring6-2)

[Spring 教程 | 分类 - 弟弟快看-教程 (ddkk.com)](https://ddkk.com/category/j2ee/spring/1/index.html)

[spring.io](https://spring.io)

# Spring

## 1 概述

### 1.3 Spring Framework特点

* 非侵入式：使用 Spring Framework 开发应用程序时，Spring 对应用程序本身的结构影响非常小。对领域模型可以做到零污染；对功能性组件也只需要使用几个简单的注解进行标记，完全不会破坏原有结构，反而能将组件结构进一步简化。这就使得基于 Spring Framework 开发应用程序时结构清晰、简洁优雅。
* 控制反转：IoC——Inversion of Control，翻转资源获取方向。把自己创建资源、向环境索取资源变成环境将资源准备好，我们享受资源注入。
* 面向切面编程：AOP——Aspect Oriented Programming，在不修改源代码的基础上增强代码功能。
* 容器：Spring IoC 是一个容器，因为它包含并且管理组件对象的生命周期。组件享受到了容器化的管理，替程序员屏蔽了组件创建过程中的大量细节，极大的降低了使用门槛，大幅度提高了开发效率。
* 组件化：Spring 实现了使用简单的组件配置组合成一个复杂的应用。在 Spring 中可以使用 XML 和 Java 注解组合这些对象。这使得我们可以基于一个个功能明确、边界清晰的组件有条不紊的搭建超大型复杂应用系统。
* 站式：在 IoC 和 AOP 的基础上可以整合各种企业应用的开源框架和优秀的第三方类库。而且 Spring 旗下的项目已经覆盖了广泛领域，很多方面的功能性需求可以在 Spring Framework 的基础上全部使用 Spring 来实现。

### 1.4、Spring模块组成

[Spring Framework Documentation :: Spring Framework](https://docs.spring.io/spring-framework/reference/index.html)

| [Overview](https://docs.spring.io/spring-framework/reference/overview.html) | History, Design Philosophy, Feedback, Getting Started.       |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| [Core](https://docs.spring.io/spring-framework/reference/core.html) | IoC Container, Events, Resources, i18n, Validation, Data Binding, Type Conversion, SpEL, AOP, AOT. |
| [Testing](https://docs.spring.io/spring-framework/reference/testing.html#testing) | Mock Objects, TestContext Framework, Spring MVC Test, WebTestClient. |
| [Data Access](https://docs.spring.io/spring-framework/reference/data-access.html) | Transactions, DAO Support, JDBC, R2DBC, O/R Mapping, XML Marshalling. |
| [Web Servlet](https://docs.spring.io/spring-framework/reference/web.html) | Spring MVC, WebSocket, SockJS, STOMP Messaging.              |
| [Web Reactive](https://docs.spring.io/spring-framework/reference/web-reactive.html) | Spring WebFlux, WebClient, WebSocket, RSocket.               |
| [Integration](https://docs.spring.io/spring-framework/reference/integration.html) | REST Clients, JMS, JCA, JMX, Email, Tasks, Scheduling, Caching, Observability, JVM Checkpoint Restore. |
| [Languages](https://docs.spring.io/spring-framework/reference/languages.html) | Kotlin, Groovy, Dynamic Languages.                           |
| [Appendix](https://docs.spring.io/spring-framework/reference/appendix.html) | Spring properties.                                           |



**①Spring Core（核心容器）**

spring core提供了IOC,DI,Bean配置装载创建的核心实现。核心概念： Beans、BeanFactory、BeanDefinitions、ApplicationContext。

- spring-core ：IOC和DI的基本实现
- spring-beans：BeanFactory和Bean的装配管理(BeanFactory)
- spring-context：Spring context上下文，即IOC容器(AppliactionContext)
- spring-expression：spring表达式语言

**②Spring AOP**

- spring-aop：面向切面编程的应用模块，整合ASM，CGLib，JDK Proxy
- spring-aspects：集成AspectJ，AOP应用框架
- spring-instrument：动态Class Loading模块

**③Spring Data Access**

- spring-jdbc：spring对JDBC的封装，用于简化jdbc操作
- spring-orm：java对象与数据库数据的映射框架
- spring-oxm：对象与xml文件的映射框架
- spring-jms： Spring对Java Message Service(java消息服务)的封装，用于服务之间相互通信
- spring-tx：spring jdbc事务管理

**④Spring Web**

- spring-web：最基础的web支持，建立于spring-context之上，通过servlet或listener来初始化IOC容器
- spring-webmvc：实现web mvc
- spring-websocket：与前端的全双工通信协议
- spring-webflux：Spring 5.0提供的，用于取代传统java servlet，非阻塞式Reactive Web框架，异步，非阻塞，事件驱动的服务

**⑤Spring Message**

- Spring-messaging：spring 4.0提供的，为Spring集成一些基础的报文传送服务

**⑥Spring test**

- spring-test：集成测试支持，主要是对junit的封装



## 2 IOC

* Inverse of Controller 控制反转，指把创建对象过程交给Spring进行管理

* Spring 中的 IoC 的实现原理就是工厂模式加反射机制。

Spring IOC 负责创建对象，管理对象（通过依赖注入（DI），装配对象，配置对象，并且管理这些对象的整个生命周期。

### 2.2 IOC的优点是什么？

- IOC 或 依赖注入把应用的代码量降到最低。
- 它使应用容易测试，单元测试不再需要单例和JNDI查找机制。
- 最小的代价和最小的侵入性使松散耦合得以实现。
- IOC容器支持加载服务时的饿汉式初始化和懒加载。

### 2.3 Spring 的 IoC支持哪些功能

- **依赖注入**
- 依赖检查
- 自动装配
- 支持集合
- 指定初始化方法和销毁方法
- 支持回调某些方法（但是需要实现 Spring 接口，略有侵入）

### 2.4 BeanFactory 和 ApplicationContext有什么区别？

BeanFactory和ApplicationContext是Spring的两大核心接口，都可以当做Spring的容器。其中ApplicationContext是BeanFactory的子接口。

#### 依赖关系

BeanFactory：是Spring里面最底层的接口，包含了各种Bean的定义，读取bean配置文档，管理bean的加载、实例化，控制bean的生命周期，维护bean之间的依赖关系。

ApplicationContext接口作为BeanFactory的派生，除了提供BeanFactory所具有的功能外，还提供了更完整的框架功能：

- 继承MessageSource，因此支持国际化。
- 统一的资源文件访问方式。
- 提供在监听器中注册bean的事件。
- 同时加载多个配置文件。
- 载入多个（有继承关系）上下文 ，使得每一个上下文都专注于一个特定的层次，比如应用的web层。



#### 加载方式

BeanFactroy采用的是延迟加载形式来注入Bean的，即只有在使用到某个Bean时(调用getBean())，才对该Bean进行加载实例化。这样，我们就不能发现一些存在的Spring的配置问题。如果Bean的某一个属性没有注入，BeanFacotry加载后，直至第一次使用调用getBean方法才会抛出异常。



ApplicationContext，它是在容器启动时，一次性创建了所有的Bean。这样，在容器启动时，我们就可以发现Spring中存在的配置错误，这样有利于检查所依赖属性是否注入。 ApplicationContext启动后预载入所有的单实例Bean，通过预载入单实例bean ,确保当你需要的时候，你就不用等待，因为它们已经创建好了。



#### 创建方式

BeanFactory通常以编程的方式被创建，ApplicationContext还能以声明的方式创建，如使用ContextLoader。



#### 注册方式

BeanFactory和ApplicationContext都支持**BeanPostProcessor**、**BeanFactoryPostProcessor**的使用，但两者之间的区别是：BeanFactory需要手动注册，而ApplicationContext则是自动注册。

## 3 AOP

Aspect Oriented Programming，面向切面编程。AOP用来封装多个类的公共行为，将那些与业务无关，却为业务模块所共同调用的逻辑封装起来，减少系统的重复代码，降低模块间的耦合度。另外，AOP还解决一些系统层面上的问题，比如日志、事物、权限等



## 4 Spring Beans

Spring beans 是那些形成Spring应用的主干的java对象。它们被Spring IOC容器初始化，装配，和管理。这些beans通过容器中配置的元数据创建。比如，以XML文件中 的形式定义。

#### 一个 Spring Bean 定义 包含什么？

一个Spring Bean 的定义包含容器必知的所有配置元数据，包括如何创建一个bean，它的生命周期详情及它的依赖。

### 如何给Spring 容器提供配置元数据？Spring有几种配置方式

这里有三种重要的方法给Spring 容器提供配置元数据。

- XML配置文件。
- 基于注解的配置。
- 基于java的配置。

### Spring基于xml注入bean的几种方式

**1、** Set方法注入；
**2、** 构造器注入：①通过index设置参数的位置；②通过type设置参数类型；
**3、** 静态工厂注入；
**4、** 实例工厂；



### 你怎样定义类的作用域？

当定义一个 在Spring里，我们还能给这个bean声明一个作用域。它可以通过bean 定义中的scope属性来定义。如，当Spring要在需要的时候每次生产一个新的bean实例，bean的scope属性被指定为**prototype**。另一方面，一个bean每次使用的时候必须返回同一个实例，这个bean的scope 属性 必须设为 **singleton**。

### Spring框架中的单例bean是线程安全的吗？

不是，Spring框架中的单例bean不是线程安全的。

spring 中的 bean 默认是单例模式，spring 框架并没有对单例 bean 进行多线程的封装处理。

实际上大部分时候 spring bean 无状态的（比如 dao 类），所有某种程度上来说 bean 也是安全的，但如果 bean 有状态的话（比如 view model 对象），那就要开发者自己去保证线程安全了，最简单的就是改变 bean 的作用域，把“singleton”变更为“prototype”，这样请求 bean 相当于 new Bean()了，所以就可以保证线程安全了。

- 有状态就是有数据存储功能。
- 无状态就是不会保存数据。

### Spring如何处理线程并发问题？

在一般情况下，只有无状态的Bean才可以在多线程环境下共享，在Spring中，绝大部分Bean都可以声明为singleton作用域，**因为Spring对一些Bean中非线程安全状态采用ThreadLocal进行处理，解决线程安全问题**。

ThreadLocal和线程同步机制都是为了解决多线程中相同变量的访问冲突问题。同步机制采用了“时间换空间”的方式，仅提供一份变量，不同的线程在访问前需要获取锁，没获得锁的线程则需要排队。而ThreadLocal采用了“空间换时间”的方式。

ThreadLocal会为每一个线程提供一个独立的变量副本，从而隔离了多个线程对数据的访问冲突。因为每一个线程都拥有自己的变量副本，从而也就没有必要对该变量进行同步了。ThreadLocal提供了线程安全的共享对象，在编写多线程代码时，可以把不安全的变量封装进ThreadLocal。

### 解释Spring框架中bean的生命周期

1. bean 对象创建（调用无参的构造）

2. 给 bean 对象设置相关属性
3. bean 后置处理器（初始化之前）
4. bean 对象初始化
5. bean后置处理器（初始化之后）
6. bean 对象创建完成，可使用了
7. bean 对象销毁（可配置指定销毁的方法）
8. IoC容器关闭

todo ..

#### 自动装配

## 5 JUnit

事物

资源操作：Resources

国际化：I18n

数据校验：Validation

提前编译：AOT （Spring 6新特性）