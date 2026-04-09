package com.omron.oss.integration.acquisition;

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

public interface Vk70xNmcLibrary extends Library {

    int Server_TCPOpen(int port);

    int Server_Get_ConnectedClientNumbers(IntByReference count);

    int VK70xNMC_Set_SystemMode(int mci, int p1, int p2, int p3);

    int VK70xNMC_InitializeAll(int mci, int[] parameters, int parameterLength);

    int VK70xNMC_Set_BlockingMethodtoReadADCResult(int readMode, int timeout);

    int VK70xNMC_StartSampling_NPoints(int mci, int samplingNum);

    int VK70xNMC_StopSampling(int mci);

    int VK70xNMC_GetFourChannel(int mci, Pointer pointer, int samplingNum);
}
