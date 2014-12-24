# map points into blender
import bpy

pixel_spacing = 0.5
strip_gap = 5.0

initial_y_location = 0.0
initial_x_location = 0.0
initial_z_location = 0.0

current_y_location = initial_y_location
current_x_location = initial_x_location
current_z_location = initial_z_location

current_address = 0

build_list = []

coords = []

for i in range(30):
    coords.append([current_x_location,current_y_location,current_z_location])
    current_y_location = current_y_location - pixel_spacing
    current_address = current_address + 1

    
current_y_location = current_y_location - strip_gap

for i in range(64):
    coords.append([current_x_location,current_y_location,current_z_location])
    current_y_location = current_y_location - pixel_spacing
    current_address = current_address + 1
    
current_y_location = current_y_location - strip_gap

for i in range(30):
    coords.append([current_x_location,current_y_location,current_z_location])
    current_y_location = current_y_location - pixel_spacing
    current_address = current_address + 1

current_y_location = current_y_location - strip_gap
current_x_location = current_x_location + strip_gap

for i in range(64):
    coords.append([current_x_location,current_y_location,current_z_location])
    current_x_location = current_x_location + pixel_spacing
    current_address = current_address + 1

current_y_location = current_y_location + strip_gap
current_x_location = current_x_location + strip_gap

for i in range(64):
    coords.append([current_x_location,current_y_location,current_z_location])
    current_y_location = current_y_location + pixel_spacing
    current_address = current_address + 1

#create mesh and object
mesh = bpy.data.meshes.new("wave")
object = bpy.data.objects.new("wave",mesh)
 
#set mesh location
object.location = bpy.context.scene.cursor_location
bpy.context.scene.objects.link(object)
 
#create mesh from python data
mesh.from_pydata(coords,[],[])
mesh.update(calc_edges=True)