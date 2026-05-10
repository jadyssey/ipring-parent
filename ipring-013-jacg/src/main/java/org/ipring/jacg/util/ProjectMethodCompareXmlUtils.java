package org.ipring.jacg.util;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * 复用 projectMethodCompare 目录中的安全 XML 解析辅助逻辑。
 */
public final class ProjectMethodCompareXmlUtils {

    private ProjectMethodCompareXmlUtils() {
    }

    /**
     * 为 XML 解析器设置安全特性，兼容不同运行环境。
     */
    public static void setSafeXmlFeature(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (Exception ignore) {
            // Ignore unsupported parser feature in current runtime.
        }
    }

    /**
     * 展平 XML 子节点内容，保留标签结构和文本。
     */
    public static String flattenXmlChildren(Element parent) {
        StringBuilder sb = new StringBuilder();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            sb.append(flattenXmlNode(children.item(i)));
        }
        return sb.toString();
    }

    /**
     * 递归展平单个 XML 节点。
     */
    public static String flattenXmlNode(Node node) {
        if (node == null) {
            return "";
        }

        if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
            return node.getTextContent();
        }

        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return "";
        }

        Element element = (Element) node;
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(element.getTagName());

        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            sb.append(" ")
                    .append(attr.getNodeName())
                    .append("=\"")
                    .append(attr.getNodeValue())
                    .append("\"");
        }
        sb.append(">");

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            sb.append(flattenXmlNode(children.item(i)));
        }
        sb.append("</").append(element.getTagName()).append(">");
        return sb.toString();
    }
}
