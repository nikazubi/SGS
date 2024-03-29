import FormModal from "../../../components/modal/FormModal";
import useCreateAcademyClass from "./useCreateAcademyClass";
import useUpdateAcademyClass from "./useUpdateAcademyClass";
import AcademyClassForm from "./AcademyClassForm";
import {ModalOpenMode} from "../../../utils/constants";


const initialValues = {
    queryKey: ""
};

const AcademyClassModal = ({open, subject, onClose, modalOpenMode, submitButton, ...props}) => {
    const {mutate: onCreate} = useCreateAcademyClass();
    const {mutate: onUpdate} = useUpdateAcademyClass();

    return (
        <FormModal
            open={open}
            title={"დამატება"}
            cancelText={'დახურვა'}
            saveText={'შენახვა'}
            width={700}
            height={350}
            maxWidth={700}
            maxHeight={350}
            initialValues={

                {
                    ...initialValues,
                    ...subject,
                }
            }
            formProps={{
                modalOpenMode
            }}
            form={AcademyClassForm}
            // validationSchema={validateNewsFormData(!!news, t)}
            onSubmit={modalOpenMode === ModalOpenMode.add ? onCreate : onUpdate}
            onClose={onClose}
            submitButton={submitButton}
            {...props}
        />
    );
};

export default AcademyClassModal;