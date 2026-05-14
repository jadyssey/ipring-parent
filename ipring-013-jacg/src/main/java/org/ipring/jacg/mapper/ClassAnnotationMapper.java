package org.ipring.jacg.mapper;

import org.ipring.jacg.mapper.po.JacgClassAnnotationPO;
import org.ipring.jacg.mapper.po.JacgFormatedSqlVO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * @author liuguangjin
 * @date 2025/12/23
 **/
@Mapper
public interface ClassAnnotationMapper {

    JacgClassAnnotationPO selectByClassAndAnno(String simpleClassName, String annotationName);
    List<JacgFormatedSqlVO> selectAllFormatedSql();
}
