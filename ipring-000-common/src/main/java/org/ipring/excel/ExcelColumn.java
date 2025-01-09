package org.ipring.excel;



import java.lang.annotation.*;

/**
 * 标注属性代表 excel 的哪一列
 *
 * @author YuanWenkai
 * @date 2020/7/8
 * @see ExcelUtils
 */
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelColumn {

    /**
     * @return 第几列, 同一类中需要从 0 开始连续排列
     */
    int value();
}
