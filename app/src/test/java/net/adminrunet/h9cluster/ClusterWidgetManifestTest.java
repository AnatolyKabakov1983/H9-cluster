package net.adminrunet.h9cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Manifest contract of the widget receivers and their data service. */
public final class ClusterWidgetManifestTest {
    private static final String ANDROID_NAMESPACE =
            "http://schemas.android.com/apk/res/android";
    private static final String APPWIDGET_UPDATE_ACTION =
            "android.appwidget.action.APPWIDGET_UPDATE";

    @Test
    public void compactWidgetReceiverIsRegistered() throws Exception {
        Element receiver = findReceiver(".CompactClusterWidgetProvider");
        assertNotNull(receiver);
        assertEquals("false", receiver.getAttributeNS(ANDROID_NAMESPACE, "exported"));
        assertEquals("@string/widget_compact_label", receiver.getAttributeNS(ANDROID_NAMESPACE, "label"));
        assertHasUpdateAction(receiver);
        assertEquals(
                "@xml/widget_cluster_compact_info",
                widgetProviderMetaResource(receiver));
    }

    @Test
    public void fullWidgetReceiverIsRegistered() throws Exception {
        Element receiver = findReceiver(".FullClusterWidgetProvider");
        assertNotNull(receiver);
        assertEquals("false", receiver.getAttributeNS(ANDROID_NAMESPACE, "exported"));
        assertEquals("@string/widget_full_label", receiver.getAttributeNS(ANDROID_NAMESPACE, "label"));
        assertHasUpdateAction(receiver);
        assertEquals(
                "@xml/widget_cluster_full_info",
                widgetProviderMetaResource(receiver));
    }

    @Test
    public void widgetDataServiceIsInternal() throws Exception {
        Element service = findService(".ClusterWidgetService");
        assertNotNull(service);
        assertEquals("false", service.getAttributeNS(ANDROID_NAMESPACE, "exported"));
    }

    @Test
    public void mainScreenPanelOverlayPermissionIsDeclared() throws Exception {
        File manifest = new File("src/main/AndroidManifest.xml");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        NodeList permissions = factory.newDocumentBuilder()
                .parse(manifest)
                .getElementsByTagName("uses-permission");
        for (int index = 0; index < permissions.getLength(); index++) {
            Element permission = (Element) permissions.item(index);
            if ("android.permission.SYSTEM_ALERT_WINDOW".equals(
                    permission.getAttributeNS(ANDROID_NAMESPACE, "name"))) {
                return;
            }
        }
        throw new AssertionError(
                "Missing SYSTEM_ALERT_WINDOW for the main-screen panel");
    }

    private static Element findReceiver(String name) throws Exception {
        return findComponent("receiver", name);
    }

    private static Element findService(String name) throws Exception {
        return findComponent("service", name);
    }

    private static Element findComponent(String tag, String name) throws Exception {
        File manifest = new File("src/main/AndroidManifest.xml");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        NodeList components = factory.newDocumentBuilder()
                .parse(manifest)
                .getElementsByTagName(tag);
        for (int index = 0; index < components.getLength(); index++) {
            Element component = (Element) components.item(index);
            if (name.equals(
                    component.getAttributeNS(ANDROID_NAMESPACE, "name"))) {
                return component;
            }
        }
        return null;
    }

    private static void assertHasUpdateAction(Element receiver) {
        NodeList actions = receiver.getElementsByTagName("action");
        for (int index = 0; index < actions.getLength(); index++) {
            Element action = (Element) actions.item(index);
            if (APPWIDGET_UPDATE_ACTION.equals(
                    action.getAttributeNS(ANDROID_NAMESPACE, "name"))) {
                return;
            }
        }
        throw new AssertionError("Missing APPWIDGET_UPDATE intent filter");
    }

    private static String widgetProviderMetaResource(Element receiver) {
        NodeList metaData = receiver.getElementsByTagName("meta-data");
        for (int index = 0; index < metaData.getLength(); index++) {
            Element meta = (Element) metaData.item(index);
            if ("android.appwidget.provider".equals(
                    meta.getAttributeNS(ANDROID_NAMESPACE, "name"))) {
                return meta.getAttributeNS(ANDROID_NAMESPACE, "resource");
            }
        }
        throw new AssertionError("Missing appwidget provider meta-data");
    }
}
