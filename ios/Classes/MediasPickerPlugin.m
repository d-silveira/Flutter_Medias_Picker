#import "MediasPickerPlugin.h"
#import <media_picker/media_picker-Swift.h>

@implementation MediasPickerPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftMediasPickerPlugin registerWithRegistrar:registrar];
}
@end
