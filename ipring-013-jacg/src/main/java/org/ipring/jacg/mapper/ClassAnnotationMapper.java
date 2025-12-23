package org.ipring.jacg.mapper;

import org.ipring.jacg.mapper.po.JacgClassAnnotationPO;
import org.mapstruct.Mapper;

/**
 * @author liuguangjin
 * @date 2025/12/23
 **/
@Mapper
public interface ClassAnnotationMapper {

    JacgClassAnnotationPO selectByClassAndAnno(String simpleClassName, String annotationName);
}
