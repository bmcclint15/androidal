```
    ___              __           _     _____    __ 
   /   |  ____  ____/ /________  (_)___/ /   |  / / 
  / /| | / __ \/ __  / ___/ __ \/ / __  / /| | / /  
 / ___ |/ / / / /_/ / /  / /_/ / / /_/ / ___ |/ /___
/_/  |_/_/ /_/\__,_/_/   \____/_/\__,_/_/  |_/_____/
                                                    
```
# AndroidAL
An [OpenAL](https://www.openal.org/) implementation for Android devices using only Android standard library resources.  Specifically [AudioTrack](https://developer.android.com/reference/android/media/AudioTrack) for streaming and mixing audio.

## Tested On
The few devices that have been tested using AndroidAL and the results are consistent.
1) Samsung S22
2) Samsung S9
3) Samsung Galaxy Tab A8
4) Samsung Galaxy Tab A
5) Android Studio Emulator Pixel 7 Pro Ubuntu 22.04

## Why

*TLDR* - Trying to get the JNI implementation of OpenAL working was not going well. Could not get past the exceptions for missing files although they were in the APKs and seemingly in the correct location. Thought...would this really be that hard to do? Two weeks later the initial implementation of this was completed in the summer of 2020.

In developing a game engine in Java for both Desktop and Android support, the time came to implement sound. I had used OpenAL in past projects and had lots of code examples lying around. I thought, this will not be hard and implemented the Desktop variant using OpenAL in rather short order. Following the pattern I used for the OpenGL interface to bridge the Desktop and Android classes using an abstraction layer, I started down the Android OpenAL JNI integration into the project. That did not go so well. After about two weeks of road block after road block, forum posting, pulling hair I said to myself...'OK...I did this before back in the early 90s with digital audio to the PC speaker and DMA transfers and manual mixing so really how hard is this?' Now that solution sounded terrible then BUT the concept is the same. So, what Android library objects exist that will allow for that type of operation? After some implementation of a few audio objects in Android I settled on the AudioTrack. In a few days I had the basics working, then after a few more days a working implementation, although it was very custom and the bridge was not that good. But, positional audio and distance fading work, ambient sounds, looping, playing and pausing, enough for a game.  So it sat like that for over three years as 'it did the job'.

Fast forward to end 2023 and I decided to complete the abstraction. Try to mimic the OpenAL specification as closely as possible to make the code for both Desktop and Android as close as possible. This is the result. The bridge class is now essentially the same between Desktop and Android differing in namespaces mostly.

So how does it work...it works OK.

Now the hard stuff...

1) Is it great.  NO.  It needs improvement BUT generally works well.
2) *Sometimes* there is an audible popping.  I think this is a seaming issue in the frames as each new frames goes into the mixer.  They may be off by a few samples.  I have see 10 - 30 sample jitter in each thread pass.  I *think* that's where its coming from.  It is more frequent in the emulator than the physical devices.
3) Its not a complete implementation.  See below BUT the essentials are there.  There are lots of API calls that have no application, some that are a differing data type than another and really 'probably' are not used much.  Then there are extensions, capturing, etc.  So lots to do *if* the need / urge arises.
4) Are there existing solutions.  Absolutely.  This is just another.  I'd suspect that *most* are better.  But...this is OpenAL in an out of the box solution.  See the examples below.
5) There are probably better ways to do this.  More optimizations.  Ways to write the Java to compile into faster executions.  Its not slow though. Time for some numbers. The default implementation is a refresh rate of 46hz (I forget where this number came from). Most devices have 44100Hz or 48000Hz sample rates.  So 46hz is roughly 21.74 milliseconds per cycle.  At the two rates, samples per cycle are ~959 and ~1044 respectively.  Internally everything is a float in the -1.0 to 1.0 range.  Default output is stereo so roughly 8k bytes per mixing loop.  Timings at this time are around 100 microseconds to process the sound data of two to four active samples.  Lots of room to grow.
6) My audio implementations are certainly not correct or naive at the best.  At this time distance falloff, stereo panning and Doppler shift are computed.  It works, they may be more robust solutions.
7) Do I hope the community like it...sure...I welcome the feedback.  Maybe it cam be made better.  Maybe it sill a niche.  Its super simple and the project is very light...so it should be easy for someone to try out.  So please...try it out.

## Legal Stuff

OK...this is my first 'community' project.  When I wrote this those years ago a colleague suggested I post it.  Well...Mike...here we are.  Now...what are the legalities? I am not trying to profit from this.  Not trying to step on OpenAL or any of their licenses.  This was something I needed, something maybe others could use.  If there are any issues with this, I will gladly take the project down.  I'd like to hear what folks think.  How would one tell me...maybe there is a way to message in GitLab.  I have not tried.  If you are reading this...you must be on GitLab.  Anyway...let me know.

## Design

Note: The entire project is implemented as a package-private resource.  Only parts are 'externally' accessible.  The AndroidAL, AL and ALC objects are visible. All other objects are internal to the library.  So the API is pretty straight forward.  AL and ALC are the standard OpenAL enumerations.  AndroidAL is the API and all that is exposed are the al* and alc* calls.  So essentially...OpenAL.

- AndroidAL.java - Main driver where all the below methods are implemented.  This object contains the list of devices, contexts and buffers.  Additionally it houses the mixer, error states and current device and context for easy reference.
- AL.java - AL enumeration maintaining the OpenAL integer values.
- ALC.java - ALC enumeration maintaining the OpenAL integer values.
- AudioDevice.java - Where the AudioTrack is implemented.
- AudioContext.java - Simulated OpenAL context object containing sources, listener and effect variables.
- AudioMixer.java - This is where the active sources are manipulated and mixed.
- AudioBuffer.java - Implementation of the OpenAL buffer object.
- AudioSource.java - Implementation of the OpenAL source object.
- AudioListener.java - Implementation of the OpenAL listener object.
- AudioUtilities.java - This is where the effect methods reside and resampling utilities.
- Utilities.java - Basic arithmetic and data type conversion methods.

## Engine Integration
Basic diagram illustrating my engines external touch points where hardware endpoints are hit and where AndroidAL fits in its architecture.
```
Device spec :  Shared resources
  GR        :                NET                                         
    \       : \ | /         /                                            
*-AU--DA-\  :  \|/     /-EL-- GR                                            
    /     \ :  APn    /     \                                            
  IN       \:   |    /       AU                                            
       D?---+---+---+---E?                                         
  GR       /:   |    \       REN                                            
    \     / :   |     \     /                                            
^-AU--DD-/  :  RSC     \-EA--MDL                                         
    /       :  / \          \                                           
  IN        :FLE  ZIP        SHD                                        

* = AndroidAL (where this library integrates)
^ = Desktop variant using LWJGL OpenAL components.

(AU)dio, (GR)aphics, (IN)put, (NET)work
(AP)plicationX, (EL)ngLibrary, (EA)ngAssets
(DD)evDesktop, (DA)evAndroid, D?(evOther)
```
Its worth stating that the DevDesktop and DevAndroid projects are only 6 or 8 files each, just to wire the touch points.  All the Application and Engine libraries are shared resources.

## Example(s)
What follows are a few examples of how the code relates to another implementation.  Specifically, the desktop variant is using [LWJGL](https://www.lwjgl.org/).  Following the [programmers reference](https://www.openal.org/documentation/OpenAL_Programmers_Guide.pdf) should translate to this implementation.

Example: Open device and context, read Vendor and close.
```code
[LWJGL]
// No object to create in LWJGL...
device = ALC10.alcOpenDevice((ByteBuffer) null);
context = ALC10.alcCreateContext(device, attributes);
ALC10.alcMakeContextCurrent(context);
alVendor = AL10.alGetString(AL10.AL_VENDOR);
ALC10.alcMakeContextCurrent(-1);
ALC10.alcDestroyContext(context);
ALC10.alcCloseDevice(device);

vs.

[AndroidAL]
AAL = new AndroidAL(); // <-- Custom OpenAL for Android
device = (int) AAL.alcOpenDevice(null);
context = (int) AAL.alcCreateContext(device, attributes);
AAL.alcMakeContextCurrent(context);
alVendor = ALL.alGetString(AL.AL_VENDOR);
AAL.alcMakeContextCurrent(-1);
AAL.alcDestroyContext(context);
AAL.alcCloseDevice(device);
```

## Implementation
Much of the API, according to the [programmers reference](https://www.openal.org/documentation/OpenAL_Programmers_Guide.pdf), have been implemented here as faithfully as possible.  Some methods may differ from the 'specification' but only slightly.  See below for the list of methods and their respective status'.

**N/A** = There are no relevant properties defined in OpenAL 1.1 which can be affected by this call, but this function may be used by OpenAL extensions.

*NOT IMPLEMENTED* are either they don't make much sense OR just a matter of working it out.  Overall, whats here is enough to get things working.

#### BUFFER FUNCTIONS
- alGenBuffers - Implemented - Generates N simulated buffer Ids.
- alDeleteBuffers - Implemented - Releases N buffers and removes them from tracking.
- alIsBuffer - Implemented - Is the buffer Id a known buffer.
- alBufferData - Implemented - Creates an AudioBuffer, resamples the data and tracks the buffer.
- alBufferf - **N/A**
- alBuffer3f - **N/A**
- alBufferfv - **N/A**
- alBufferi - **N/A**
- alBuffer3i - **N/A**
- alBufferiv - **N/A**
- alGetBufferf - **N/A**
- alGetBuffer3f - **N/A**
- alGetBufferfv - **N/A**
- alGetBufferi - Implemented - Returns the integer value of the specified parameter.
- alGetBuffer3i - **N/A**
- alGetBufferiv - *NOT IMPLEMENTED* - Use getBufferi instead.

#### SOURCE FUNCTIONS
- alGenSources - Implemented - Generates N simulated source Ids.
- alDeleteSources - Implemented - Releases N sources and removes them from tracking.
- alIsSource - Implemented - Is the source Id a known source.
- alSourcef - Implemented - Sets the float value of a source parameter.
- alSource3f - Implemented - Sets the 3D float values of a source parameter.
- alSourcefv - Implemented - Sets the 3D float values of a source parameter.
- alSourcei - Implemented - Sets the integer value of a source parameter.
- alSource3i - *NOT IMPLEMENTED* - Use alSource3f instead.
- alSourceiv - *NOT IMPLEMENTED* - Use alSourcefv instead.
- alGetSourcef - Implemented - Returns the float value of the specified source parameter.
- alGetSource3f - *NOT IMPLEMENTED*
- alGetSourcefv - Implemented - Returns float values of a source parameter.
- alGetSourcei - Implemented - Returns the integer value of the specified source parameter.
- alGetSource3i - *NOT IMPLEMENTED*
- alGetSourceiv - *NOT IMPLEMENTED* - Use alGetSourcefv instead.
- alSourcePlay - Implemented - Sets the source state to AL_PLAYING.
- alSourcePlayv - *NOT IMPLEMENTED* - Use the single variant instead.
- alSourcePause - Implemented - Sets the source state to AL_PAUSED.
- alSourcePausev - *NOT IMPLEMENTED* - Use the single variant instead.
- alSourceStop - Implemented - Sets the source state to AL_STOPPED.
- alSourceStopv - *NOT IMPLEMENTED* - Use the single variant instead.
- alSourceRewind - Implemented - Sets the source state to AL_INITIAL.
- alSourceRewindv - *NOT IMPLEMENTED* - Use the single variant instead.
- alSourceQueueBuffers - *NOT IMPLEMENTED* - NOT DOING QUEUED BUFFERS.
- alSourceUnqueueBuffers - *NOT IMPLEMENTED* - NOT DOING QUEUED BUFFERS.

#### LISTENER FUNCTIONS 
- alListenerf - Implemented - Sets the float value of a listener parameter.
- alListener3f - Implemented - Sets the float value of a listener parameter.
- alListenerfv - Implemented - Gets the float values of a listener parameter.
- alListeneri - **N/A**
- alListener3i - *NOT IMPLEMENTED*
- alListeneriv - *NOT IMPLEMENTED* - Use alListenerfv instead.
- alGetListenerf - Implemented - Gets the float value of a listener parameter.
- alGetListener3f - *NOT IMPLEMENTED*
- alGetListenerfv - Implemented - Returns float values of a listener parameter.
- alGetListeneri - **N/A**
- alGetListener3i - *NOT IMPLEMENTED*
- alGetListeneriv - *NOT IMPLEMENTED*

#### STATE FUNCTIONS 
- alEnable - **N/A**
- alDisable - **N/A**
- alIsEnabled - **N/A**
- alGetBoolean - *NOT IMPLEMENTED* - Makes no sense.
- alGetDouble - *NOT IMPLEMENTED* - Use alGetFloat instead.
- alGetFloat - Implemented - Returns the float value of the specified parameter.
- alGetInteger - *NOT IMPLEMENTED* - Use alGetFloat instead.
- alGetBooleanv - *NOT IMPLEMENTED* - Makes no sense.
- alGetDoublev - *NOT IMPLEMENTED* - Makes no sense.
- alGetFloatv - *NOT IMPLEMENTED* - Makes no sense.
- alGetIntegerv - *NOT IMPLEMENTED* - Makes no sense.
- alGetString - Implemented - Returns the string value of the specified parameter.
- alDistanceModel - Implemented - Sets the distance attenuation model.
- alDopplerFactor - Implemented - Sets the Doppler effect factor.
- alSpeedOfSound - Implemented - Sets the speed of sound.

#### ERROR FUNCTIONS 
- alGetError - Implemented - Obtains error information.

#### EXTENSION FUNCTIONS 
- alIsExtensionPresent - *NOT IMPLEMENTED* - No extensions.
- alGetProcAddress - *NOT IMPLEMENTED* - No extensions.
- alGetEnumValue - *NOT IMPLEMENTED* - Enumeration contains a .value() method.

#### CONTEXT MANAGEMENT FUNCTIONS 
- alcCreateContext - Implemented - Creates the AudioContext and AudioMixer.  **Attributes** are *NOT IMPLEMENTED* yet.
- alcMakeContextCurrent - Implemented - Starts the AudioTrack and AudioMixer thread.
- alcProcessContext - *NOT IMPLEMENTED*
- alcSuspendContext - *NOT IMPLEMENTED*
- alcDestroyContext - Implemented - Interrupts the AudioMixer thread, clear context sources and removes the context.
- alcGetCurrentContext - Implemented - Gets the context Id that is current.
- alcGetContextsDevice - Implemented - Gets the device Id for the supplied Context Id.

#### CONTEXT ERROR FUNCTIONS
- alcGetError - Implemented - Queries ALC errors.

#### CONTEXT DEVICE FUNCTIONS 
- alcOpenDevice - Implemented - Creates an AudioDevice with a unique Id.  Id is the AudioTrack getAudioSessionId().
- alcCloseDevice - Implemented - Stops and released the AudioTrack, clears the buffers and removed device.

#### CONTEXT EXTENSION FUNCTIONS
- alcIsExtensionPresent - *NOT IMPLEMENTED* - No extensions.
- alcGetProcAddress - *NOT IMPLEMENTED* - No extensions.
- alcGetEnumValue - *NOT IMPLEMENTED* - Enumeration contains a .value() method.

#### CONTEXT STATE FUNCTIONS
- alcGetString - Implemented - Returns the string value of the specified parameter
- alcGetIntegerv - Implemented - Obtains integer value(s) from ALC.

#### CONTEXT CAPTURE FUNCTIONS
- alcCaptureOpenDevice - *NOT IMPLEMENTED* - No capturing in this release.
- alcCaptureCloseDevice - *NOT IMPLEMENTED* - No capturing in this release.
- alcCaptureStart - *NOT IMPLEMENTED* - No capturing in this release.
- alcCaptureStop - *NOT IMPLEMENTED* - No capturing in this release.
- alcCaptureSamples - *NOT IMPLEMENTED* - No capturing in this release.

#### CUSTOM FUNCTIONS (Needed a little more functionality)
- alIsError - Is the AL state in error?
- alGetErrorDescr - Retrieve the last AL error state.
- alcIsError - Is the ALC state in error?
- alcGetErrorDescr - Retrieve the last ALC error state.

## References
- https://developer.android.com/reference/android/media/AudioTrack
- https://www.openal.org/
- https://www.openal.org/documentation/OpenAL_Programmers_Guide.pdf
- https://www.openal.org/documentation/openal-1.1-specification.pdf
- https://github.com/kcat/openal-soft
- https://www.lwjgl.org/
