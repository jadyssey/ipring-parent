package org.ipring.converter;


import org.ipring.model.param.account.AccountAddParam;
import org.ipring.model.param.account.AccountUpdateParam;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 在定义完转换接口后，我们使用mvn clean compile编译工程，可以看到MapStruct框架已自动生成源码：
 * https://juejin.cn/post/7212550699322376253
 *
 * @author lgj
 * @date 2024/4/8
 **/
@Mapper
public interface BeanMapper {

    BeanMapper INSTANTCE = Mappers.getMapper(BeanMapper.class);

    AccountUpdateParam tick2Push(AccountAddParam addParam);
}