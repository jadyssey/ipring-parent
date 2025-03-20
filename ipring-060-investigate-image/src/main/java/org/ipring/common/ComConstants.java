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
    String systemSetup2 = "You are a senior logistics quality inspection expert, skilled in accurately identifying shipping labels through image features. Please follow the strict three-level verification process according to international express shipping label standards.";


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
            "Please answer these 7 questions in sequence, the answers are json object.";
    String en2_classifyQuestion_jsonResponseFormat = "{\"type\": \"object\",\"properties\": {\"q1\": { \"type\": \"boolean\" },\"q2\": { \"type\": \"string\" },\"q3\": { \"type\": \"boolean\" },\"q4\": { \"type\": \"boolean\" },\"q5\": { \"type\": \"boolean\" },\"q6\": { \"type\": \"boolean\" },\"q7\": { \"type\": \"string\" }},\"required\": [\"q1\", \"q2\", \"q3\", \"q4\", \"q5\", \"q6\", \"q7\"],\"additionalProperties\": false}";

    /**
     * 0221 优化后的提示词
     */
    String Q_0221_ONE =
            "请按步骤分析图像：\n" +
            "1. 定位检测：识别包裹外表面最大平面区域，检测是否有矩形文档类对象贴附\n" +
            "2. 要素核验：若存在，进一步在定位检测区域内是否同时包含：\n" +
            "   - 文字区块：查看图片中是否有\"收件人地址\"文本信息\n" +
            "   - 编码区块：至少1个条形码或二维码，且邻近有%s位以字母、数字组成的单号\n" +
            "   - 企业标识：快递公司LOGO或名称（如“GOFO”“CIRRO”）\n" +
            "3. 清晰度判断：上述关键要素的文本/图形需完整无遮挡，文字可提取，文字在图像分辨率下可被人类肉眼辨识\n" +
            "4. 输出结论格式：\n" +
            "  - q1: 步骤1、2、3都满足的且内容完整清晰的快递面单\n" +
            "  - q2: 置信度:0-100%\n" +
            "  - q3: 请给出分析的理由";
        String Q_0221_TWO =
            "请按步骤分析图像：\n" +
            "1. 定位检测：识别包裹外表面是否有长方形热敏纸张贴在包裹表面\n" +
            "2. 要素核验：若存在，进一步在定位检测区域内是否同时包含：\n" +
            "   - 文字区块：查看图片中是否有\"收件人地址\"文本信息\n" +
            "   - 编码区块：至少1个条形码或二维码，且邻近有%s位以字母、数字组成的单号\n" +
            "3. 清晰度判断：\n" +
            "   - 暗部无彩色噪斑\n" +
            "   - 无运动模糊\n" +
            "   - 文字可读，最小可见字号≤8pt\n" +
            "4. 输出结论格式：\n" +
            "  - q1: 步骤1、2、3都满足且内容完整清晰的快递面单\n" +
            "  - q2: 置信度:0-100%\n" +
            "  - q3: 请给出分析的理由";

    /**
     * 1000单筛出来22单，准确率100%
     */
    String Q_0221_THREE =
            "请按步骤分析图像：\n" +
            "1. 定位检测：识别包裹外表面是否有纸张贴在包裹表面\n" +
            "2. 要素核验：若存在，进一步在定位检测区域内是否同时包含：\n" +
            "   - 文字区块：查看图片中是否有\"收件人地址\"文本信息\n" +
            "   - 编码区块：至少含有一个条形码和一个二维码\n" +
            "3. 清晰度判断：\n" +
            "   - 无严重噪斑\n" +
            "   - 无运动模糊\n" +
            "   - 文字区块文字清晰可提取\n" +
            "4. 输出结论格式：\n" +
            "  - q1: 步骤1、2、3都满足且内容完整清晰的快递面单\n" +
            "  - q2: 请给出分析的理由\n" +
            "  - q3: 如果q1为true，请给出你从文字区块中提取到的数据以用于佐证你q2的理由";

    /**
     * 生产版本，去掉多余问题
     */
    String Q_0221_THREE_PROD =
            "请按步骤分析图像：\n" +
            "1. 定位检测：识别包裹外表面是否有纸张贴在包裹表面\n" +
            "2. 要素核验：若存在，进一步在定位检测区域内是否同时包含：\n" +
            "   - 文字区块：查看图片中是否有\"收件人地址\"文本信息\n" +
            "   - 编码区块：至少含有一个条形码和一个二维码\n" +
            "3. 清晰度判断：\n" +
            "   - 无严重噪斑\n" +
            "   - 无运动模糊\n" +
            "   - 文字区块文字清晰可提取\n" +
            "4. 输出结论格式：\n" +
            "  - q1: 步骤1、2、3都满足且内容完整清晰的快递面单";

    String Q_0221_5 =
            "请按步骤分析图像：\n" +
            "1. 定位检测：识别快递包裹外表面是否有纸张贴在表面，如果有则认为是快递面单\n" +
            "2. 要素核验：\n" +
            "   - 文字区块：面单上识别是否有\"收件人地址\"文本信息且无缺失\n" +
            "   - 编码区块：面单上识别至少含有一个条形码和一个二维码\n" +
            "3. 清晰度判断：\n" +
            "   - 无严重噪斑\n" +
            "   - 无运动模糊\n" +
            "   - 面单文字区块需要可识别，最小可见字号≤16pt\n" +
            "4. 输出结论格式：\n" +
            "  - q1: 输出包裹表面快递面单中提取到的地址信息，如果未输出，则认为要素核验不通过\n" +
            "  - q2: 请给出分析推理过程\n" +
            "  - q3: 步骤1、2、3都满足且内容完整无缺失的快递面单\n";

    /**
     * 三轮1000单，拉出64单，false的错了一单
     */
    String Q_0221_7 =
            "以第一张图片为示例，该图片展示了一个包裹，包裹上贴有一个运单标签，请按步骤分析其他的图片：\n" +
            "1. 定位检测：识别快递包裹外表面是否有运单标签\n" +
            "2. 要素核验：\n" +
            "   - 文字区块：识别运单标签上是否有\"收件人地址\"文本信息且无缺失\n" +
            "   - 编码区块：识别运单标签上是否有条形码或二维码\n" +
            "3. 清晰度判断：\n" +
            "   - 运单标签文字区块需要可识别，最小可见字号≤16pt\n" +
            "   - OCR识别置信度≥90%（重点检测城市/邮编/物流编号）\n" +
            "   - 条码区域分辨率≥150ppi \n" +
            "4. 审核任务的输出结论格式：\n" +
            "  - q1: 请给出详细的分析推理过程\n" +
            "  - q2: 定位检测的结果是否识别到运单标签？\n" +
            "  - q3: 多张图片中是否存在任意一张含有运单标签且符合要素核验？\n" +
            "  - q4: 多张图片中是否存在任意一张含有运单标签且符合清晰度判断？\n";

    String Q_0221_8 =
            "请按步骤分析图像：\n" +
            "1. 定位检测：识别快递包裹外表面是否有纸张贴在表面，如果有则认为是运单标签\n" +
            "2. 要素核验：\n" +
            "   - 文字区块：运单标签上识别是否有\"收件人地址\"文本信息且无缺失\n" +
            "   - 编码区块：运单标签上是否有条形码或二维码\n" +
            "3. 清晰度判断：\n" +
            "   - 运单标签文字区块需要可识别，最小可见字号≤16pt\n" +
            "   - OCR识别置信度≥90%（重点检测城市/邮编/物流编号）\n" +
            "   - 条码区域分辨率≥150ppi \n" +
            "4. 输出结论格式：\n" +
            "  - q1: 输出包裹表面运单标签中提取到的地址信息，如果未输出，则认为要素核验不通过\n" +
            "  - q2: 请给出分析推理过程\n" +
            "  - q3: 步骤1、2、3都满足且内容完整无缺失的快递面单\n";

    String Q_0319_MS = "Identify an image that contains a shipping label affixed to the surface of a parcel. The image should be clear and legible, with no significant noise or blurriness. The shipping label must include the recipient's address and at least one barcode or QR code. shippingLabelQuestion: The output should indicate whether the image meets all the specified criteria？";
    String Q_0319_ONE_Q_jsonResponseFormat = "{\"type\": \"object\",\"properties\": {\"shippingLabelQuestion\": {\"type\": \"boolean\"}},\"required\": [\"shippingLabelQuestion\"],\"additionalProperties\": false}";


    String FIVE_Q_jsonResponseFormat = "{\"type\": \"object\",\"properties\": {\"q1\": {\"type\": \"number\"},\"q2\": {\"type\": \"number\"},\"q3\": {\"type\": \"number\"},\"q4\": {\"type\": \"number\"},\"q5\": {\"type\": \"number\"}},\"required\": [\"q1\", \"q2\", \"q3\", \"q4\", \"q5\"],\"additionalProperties\": false}";
    String FOUR_Q_jsonResponseFormat = "{\"type\": \"object\",\"properties\": {\"q1\": {\"type\": \"boolean\"},\"q2\": {\"type\": \"string\"},\"q3\": {\"type\": \"string\"},\"q4\": {\"type\": \"string\"}},\"required\": [\"q1\", \"q2\", \"q3\", \"q4\"],\"additionalProperties\": false}";
    String THREE_Q_jsonResponseFormat = "{\"type\": \"object\",\"properties\": {\"q1\": {\"type\": \"boolean\"},\"q2\": {\"type\": \"string\"},\"q3\": {\"type\": \"string\"}},\"required\": [\"q1\", \"q2\", \"q3\"],\"additionalProperties\": false}";
    String ONE_Q_jsonResponseFormat = "{\"type\": \"object\",\"properties\": {\"q1\": {\"type\": \"boolean\"}},\"required\": [\"q1\"],\"additionalProperties\": false}";

}
