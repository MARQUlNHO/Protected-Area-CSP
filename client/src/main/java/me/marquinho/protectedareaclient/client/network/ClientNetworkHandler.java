package me.marquinho.protectedareaclient.client.network;

import me.marquinho.protectedareaclient.client.network.payload.ClientNotificationPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientNetworkHandler {

    private static byte[] createPayload(String action, String id) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);

        out.writeUTF(action);
        out.writeUTF(id);
        out.flush();

        return bos.toByteArray();
    }

    public static void sendPlayerEnteredArea(String areaId) {
        try {
            ClientPlayNetworking.send(new ClientNotificationPayload(createPayload("PLAYER_ENTERED_AREA", areaId)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendPlayerLeftArea(String areaId) {
        try {
            ClientPlayNetworking.send(new ClientNotificationPayload(createPayload("PLAYER_LEFT_AREA", areaId)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void requestExpel(String areaId) {
        try {
            ClientPlayNetworking.send(new ClientNotificationPayload(createPayload("REQUEST_EXPEL", areaId)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void requestReturnToArea(String areaId) {
        try {
            ClientPlayNetworking.send(new ClientNotificationPayload(createPayload("REQUEST_RETURN", areaId)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void requestCollisionNotification(String areaId, boolean isNoEntry) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);

            out.writeUTF("REQUEST_COLLISION_NOTIFICATION");
            out.writeUTF(areaId);
            out.writeBoolean(isNoEntry);
            out.flush();

            ClientPlayNetworking.send(new ClientNotificationPayload(bos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendModResponse() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);

            out.writeUTF("MOD_RESPONSE");
            out.flush();

            ClientPlayNetworking.send(new ClientNotificationPayload(bos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendPlayerCrossedFlatArea(String areaId, boolean positiveDirection) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            out.writeUTF("PLAYER_CROSSED_FLAT_AREA");
            out.writeUTF(areaId);
            out.writeBoolean(positiveDirection);
            out.flush();
            ClientPlayNetworking.send(new ClientNotificationPayload(bos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void requestDebugPage(int page) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            out.writeUTF("REQUEST_DEBUG_PAGE");
            out.writeInt(page);
            out.flush();
            ClientPlayNetworking.send(new ClientNotificationPayload(bos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}