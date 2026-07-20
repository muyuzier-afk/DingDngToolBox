package com.toolbox.ddj.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.Inet6Address

/**
 * 网络信息数据模型。所有字段均为可直接展示的字符串，缺失值统一填 "—"。
 */
data class NetworkInfo(
    val type: String,      // 连接类型：Wi-Fi / 移动数据 / 以太网 / VPN / 未连接
    val metered: String,   // 是否计费网络
    val validated: String, // 是否已验证真实联网
    val ipv4: String,      // IPv4 地址（可能多个，逗号分隔）
    val ipv6: String,      // IPv6 地址（可能多个，逗号分隔）
    val dns: String,       // DNS 服务器
    val gateway: String,   // 默认路由网关
    val iface: String,     // 网络接口名
    val mtu: String        // MTU（无稳定公开 API，恒为占位）
)

/**
 * 网络信息采集 Repository。
 *
 * 全部基于 [ConnectivityManager] 的公开 API，无需任何权限：
 * - 连接类型 / 计费 / 验证状态：[NetworkCapabilities]
 * - IP / DNS / 网关 / 接口：LinkProperties
 *
 * 刻意不读取 Wi-Fi SSID —— 高版本系统读取 SSID 需要定位权限，得不偿失。
 */
class NetworkInfoRepository(private val context: Context) {

    suspend fun get(): NetworkInfo = withContext(Dispatchers.IO) {
        val na = "—" // 统一占位符
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(net)
        val lp = cm.getLinkProperties(net)

        // 连接类型：按 Wi-Fi > 移动 > 以太网 > VPN 的顺序判定
        val type = when {
            caps == null -> "未连接"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "移动数据"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "以太网"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "未连接"
        }

        // 是否计费：具备 NOT_METERED 能力即为不计费
        val metered =
            if (caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true) "否" else "是"
        // 是否已验证：系统确认该网络可真实访问外网
        val validated =
            if (caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true) "是" else "否"

        // 分类收集本机链路地址
        val ipv4 = mutableListOf<String>()
        val ipv6 = mutableListOf<String>()
        lp?.linkAddresses?.forEach { link ->
            when (val addr = link.address) {
                is Inet4Address -> addr.hostAddress?.let { ipv4.add(it) }
                is Inet6Address -> addr.hostAddress?.let { ipv6.add(it) }
            }
        }

        // DNS 服务器列表
        val dns = lp?.dnsServers
            ?.mapNotNull { it.hostAddress }
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(", ")

        // 默认路由对应的网关地址
        val gateway = lp?.routes
            ?.firstOrNull { it.isDefaultRoute }
            ?.gateway
            ?.hostAddress

        NetworkInfo(
            type = type,
            metered = metered,
            validated = validated,
            ipv4 = ipv4.joinToString(", ").ifEmpty { na },
            ipv6 = ipv6.joinToString(", ").ifEmpty { na },
            dns = dns ?: na,
            gateway = gateway ?: na,
            iface = lp?.interfaceName ?: na,
            // MTU 无稳定公开 API 可取，且约定不使用反射，故恒为占位
            mtu = na
        )
    }
}
