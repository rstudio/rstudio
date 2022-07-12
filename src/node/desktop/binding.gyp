{
    "targets": [
        {
            "target_name": "desktop",
            "cflags!": [ "-fno-exceptions" ],
            "cflags_cc!": [ "-fno-exceptions" ],
            "sources": [ "src/native/desktop/desktop.cc" ],
            "include_dirs": [
                "<!@(node -p \"require('node-addon-api').include\")"
            ],
            "defines": [ "NAPI_DISABLE_CPP_EXCEPTIONS" ]
        },
        {
            "target_name": "dock",
            "cflags!": [ "-fno-exceptions" ],
            "cflags_cc!": [ "-fno-exceptions" ],
            "include_dirs": [
              "<!@(node -p \"require('node-addon-api').include\")",
              "",
            ],
            "defines": [ "NAPI_DISABLE_CPP_EXCEPTIONS" ],
            "sources": ["src/native/dock/dock.cc"],
            "conditions": [
              [
                'OS=="mac"',
                {
                  "sources": ["src/native/dock/dock_mac.mm"],
                  "link_settings": {
                    "libraries": ["AppKit.framework"]
                  },
                }
              ]
            ]
        },
        {
            "target_name": "copy",
            "type": "none",
            "dependencies": [ "desktop", "dock" ],
            "copies": [
                {
                    "files": [
                      "<(module_root_dir)/build/Release/desktop.node",
                      "<(module_root_dir)/build/Release/dock.node",
                    ],
                    "destination": "<(module_root_dir)/src/native"
                }
            ],
        },
    ]
}
