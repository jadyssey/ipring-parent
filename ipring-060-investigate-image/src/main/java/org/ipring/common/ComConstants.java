package org.ipring.common;

/**
 * @author liuguangjin
 * @date 2025/1/9
 */
public interface ComConstants {

    /**
     * 场景四：投递到信箱柜
     */
    String question04 = "假设你是一个正在送快递的快递员，公司规定包裹妥投场景为“投递到信箱柜/储物格”时需要拍摄留证的照片，以下是对留证的照片内容要求：" +
            "问题1. 识别图片中是否存在包裹，若是则为 true，否则为 false；" +
            "问题2. 识别图片中的包裹上是否存在'%s'编号，若是则输出识别到的编号，否则为空；" +
            "问题3. 识别图片中包裹是否被放置在信箱柜/储物格附近，若是则为 true，否则为 false。" +
            "响应结果以‘::’形式分割，例如‘true::Abc123::false’";

    /**
     * 场景二：投递给收件人本人
     * 场景五：他人代收
     *
     */
    String question02 = "假设你是一个正在送快递的快递员，公司规定包裹妥投场景为“投递给收件人本人”时需要拍摄留证的照片，以下是对留证的照片内容要求：" +
            "问题1. 识别图片中是否存在包裹，若是则为 true，否则为 false；" +
            "问题2. 识别图片中包裹上是否包含'%s'编号，若是则输出识别到的编号，否则为空；" +
            "问题3. 识别图片中是否有包裹交接动作，若是则为 true，否则为 false。" +
            "响应结果以‘::’形式分割，例如‘true::Abc123::false’";

    /**
     * 场景三：投递到门卫or收发室
     */
    String question03 = "假设你是一个正在送快递的快递员，公司规定包裹妥投场景为“投递给收件人本人”时需要拍摄留证的照片，以下是对留证的照片内容要求：" +
            "问题1. 识别图片中是否存在包裹，若是则为 true，否则为 false；" +
            "问题2. 识别图片中包裹上是否包含'%s'编号，若是则输出识别到的编号，否则为空；" +
            "问题3. 识别图片中是否存在住宅门，若是则为 true，否则为 false；" +
            "问题4. 识别图片中是否有包裹交接动作，若是则为 true，否则为 false。" +
            "响应结果以‘::’形式分割，例如‘true::Abc123::false’";


    /**
     * 场景一：投递到家门口
     */
    String question01 = "假设你是一个正在送快递的快递员，公司规定包裹妥投场景为“投递到家门口”时需要拍摄留证的照片，以下是对留证的照片内容要求：" +
            "问题1. 识别图片中是否存在包裹，若是则为 true，否则为 false；" +
            "问题2. 识别图片中包裹上是否包含'%s'编号，若是则输出识别到的编号，否则为空；" +
            "问题3. 识别图片中包裹是否放置在住宅门附近，若是则为 true，否则为 false；" +
            "问题4. 识别图片中是否存在街道/楼道/门牌号，若是则为 true，否则为 false。" +
            "响应结果以‘::’形式分割，例如‘true::Abc123::false’";


    /**
     * 统一问题
     */
    String classifyQuestion = "问题1. 图片中是否存在清晰的面单，若是则为 true，否则为 false；问题2. 识别图片中包裹上是否包含'%s'编号，若是则输出识别到的编号，否则为空；问题3. 图片中是否存在包裹放置位置，若是则为 true，否则为 false；问题4. 包裹是否放置在住宅门附近，若是则为 true，否则为 false; 问题5. 包裹是否放置在邮箱或其他容器内，若是则为 true，否则为 false;问题6. 图片中是否存在证件照片，若是则为 true，否则为 false;问题7. 图片中是否存在清晰的人物面部，若是则为 true，否则为 false; 问题8. 图片中是否存在包裹交接动作或收件人手持包裹动作，若是则为 true，否则为 false; 响应结果以‘::’形式分割，例如‘true::Abc123::false’";
    String en_classifyQuestion = "Question 1: Is there a clear shipping label in the image? If yes, answer true; otherwise, false. Question 2: Does the image contain the package with the number '%s'? If yes, output the identified number; otherwise, leave it blank. Question 3: Is there a clear indication of the package's placement in the image? If yes, answer true; otherwise, false. Question 4: Is the package placed near the residential door? If yes, answer true; otherwise, false. Question 5: Is the package placed inside a mailbox or other container? If yes, answer true; otherwise, false. Question 6: Does the image contain an ID photo? If yes, answer true; otherwise, false. Question 7: Does the image contain a clear face of a person? If yes, answer true; otherwise, false. Question 8: Does the image show a package handover action or the recipient holding the package? If yes, answer true; otherwise, false. The response format should be separated by ‘::’, e.g., ‘true::Abc123::false’.";


    /**
     * 系统设定
     */
    String systemSetup = "Suppose you are an auditor, and you need to review the pictures taken by the courier when the parcel is properly delivered.";

    /**
     * 提示词
     */
    String en2_classifyQuestion =
            // 清晰面单
            "Question 1. Is the proper address of the printed sheet on the express parcel valid and legible? If yes, then true; otherwise, false.\n" +
            // 运单号识别
            "Question 2. Identify whether there is a code with the length of %s starting with '%s' on the express parcel, and if so, output the code; Otherwise, the symbol'-'is output.\n" +
            // 位置可识别
            "Question 3. Is the package placed in any location that can be identified by the recipient? If yes, then true; otherwise, false.\n" +
            // 放门口
            "Question 4. Is the package placed close to the front door? If yes, then true; otherwise, false.\n" +
            // 放邮箱
            "Question 5. Is the package placed in a mailbox or other container? If yes, then true; otherwise, false.\n" +
            // 交接动作
            "Question 6. Is there anyone handing over a package to someone else in the picture Or the recipient has the package in their hands? If yes, then true; otherwise, false.\n" +
            // 识别街道号码:
            "Question 7. Please only extract the street/house/mailbox number from the real scene in the picture, and ignore all numbers on the printed document. If a valid house number exists, output that number; otherwise, output the symbol '-'.\n" +
            "Please answer these 7 questions in sequence, the answers are separated by the English colon ':'. The output format is such as 'true:GO123:false:111', to ensure that the answer is accurate and the format is correct.";
}
